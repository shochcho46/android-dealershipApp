package com.example.dealernest.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.location.LocationProvider
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.qr.extractVendorSearchTerm
import com.example.dealernest.core.session.SessionManager
import com.example.dealernest.core.session.SessionState
import com.example.dealernest.domain.model.Product
import com.example.dealernest.domain.model.Vendor
import com.example.dealernest.domain.repository.OrderItemInput
import com.example.dealernest.domain.repository.OrderRepository
import com.example.dealernest.domain.repository.ProductRepository
import com.example.dealernest.domain.repository.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class OrderFormViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val vendorRepository: VendorRepository,
    private val productRepository: ProductRepository,
    private val sessionManager: SessionManager,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderFormUiState())
    val uiState: StateFlow<OrderFormUiState> = _uiState.asStateFlow()

    private var editingOrderId: Int? = null
    private var currentUserId: Int? = null
    private var vendorSearchJob: Job? = null
    private var productSearchJob: Job? = null

    // Same "search+limit only, capped at 50" API shape as the Vendor tab - "load more" raises
    // these instead of paging. See VendorListViewModel / .claude/docs/api-contract.md.
    private var vendorLimit = SEARCH_LIMIT
    private var productLimit = SEARCH_LIMIT

    init {
        viewModelScope.launch {
            // Bounded - see the matching comment in OrderListViewModel.init.
            val loggedIn = withTimeoutOrNull(8_000L) {
                sessionManager.sessionState.first { it is SessionState.LoggedIn }
            } as? SessionState.LoggedIn
            currentUserId = loggedIn?.profile?.id
        }
    }

    fun loadForEdit(orderId: Int) {
        if (editingOrderId == orderId) return
        editingOrderId = orderId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExisting = true) }
            when (val result = orderRepository.getById(orderId)) {
                is NetworkResult.Success -> {
                    val order = result.data
                    val lines = order.items.map { item ->
                        OrderCartLine(
                            product = Product(
                                id = item.productId,
                                name = item.productName,
                                imageUrl = item.productImageUrl,
                                quantityAvailable = Int.MAX_VALUE,
                                sellPrice = item.sellPrice.toDoubleOrNull() ?: 0.0,
                                // Unknown for a pre-existing line - the order-item response has
                                // no stock/min-price data, only the search response does. No
                                // floor is enforced when editing a line loaded this way.
                                minSellPrice = 0.0,
                                unitName = null,
                            ),
                            quantityText = item.quantity.toString(),
                            sellPriceText = item.sellPrice,
                            discountText = item.discountPrice,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            vendor = order.toVendorSelection(),
                            cartLines = lines,
                            isLoadingExisting = false,
                            isReadOnly = !order.isEditable,
                        )
                    }
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoadingExisting = false, errorMessage = result.message)
                }
                is NetworkResult.Exception -> _uiState.update {
                    it.copy(isLoadingExisting = false, errorMessage = "Couldn't load this order.")
                }
            }
        }
    }

    fun onVendorQueryChange(value: String) {
        _uiState.update { it.copy(vendorQuery = value) }
        vendorLimit = SEARCH_LIMIT
        vendorSearchJob?.cancel()
        vendorSearchJob = viewModelScope.launch {
            delay(300)
            searchVendor()
        }
    }

    fun onVendorQrScanned(value: String) = onVendorQueryChange(extractVendorSearchTerm(value))
    fun onVendorSelected(vendor: Vendor) = _uiState.update {
        it.copy(vendor = vendor.toOrderSelection(), vendorResults = emptyList(), vendorQuery = "")
    }
    fun onChangeVendor() = _uiState.update { it.copy(vendor = null) }

    fun loadMoreVendors() {
        if (vendorLimit >= MAX_SEARCH_LIMIT) return
        vendorLimit = (vendorLimit + SEARCH_LIMIT).coerceAtMost(MAX_SEARCH_LIMIT)
        vendorSearchJob?.cancel()
        vendorSearchJob = viewModelScope.launch { searchVendor() }
    }

    fun onProductQueryChange(value: String) {
        _uiState.update { it.copy(productQuery = value) }
        productLimit = SEARCH_LIMIT
        productSearchJob?.cancel()
        productSearchJob = viewModelScope.launch {
            delay(300)
            searchProduct()
        }
    }

    fun loadMoreProducts() {
        if (productLimit >= MAX_SEARCH_LIMIT) return
        productLimit = (productLimit + SEARCH_LIMIT).coerceAtMost(MAX_SEARCH_LIMIT)
        productSearchJob?.cancel()
        productSearchJob = viewModelScope.launch { searchProduct() }
    }

    fun onAddProduct(product: Product) = _uiState.update { state ->
        if (state.cartLines.any { it.product.id == product.id }) return@update state
        state.copy(
            cartLines = state.cartLines + OrderCartLine(product, sellPriceText = product.sellPrice.toString()),
            productQuery = "",
            productResults = emptyList(),
        )
    }

    fun onRemoveLine(productId: Int) = _uiState.update {
        it.copy(cartLines = it.cartLines.filterNot { line -> line.product.id == productId })
    }

    fun onQuantityTextChange(productId: Int, text: String) = updateLine(productId) { it.copy(quantityText = text) }
    fun onSellPriceTextChange(productId: Int, text: String) = updateLine(productId) { it.copy(sellPriceText = text) }
    fun onDiscountTextChange(productId: Int, text: String) = updateLine(productId) { it.copy(discountText = text) }

    fun submit() {
        val state = _uiState.value
        val vendor = state.vendor
        val userId = currentUserId
        if (vendor == null || userId == null || state.cartLines.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select a vendor and add at least one product.") }
            return
        }
        // vendor.id can be null when it came from an existing order the backend null-shelled
        // the vendor reference on (see OrderVendorSelection) - refuse to submit rather than
        // silently sending a wrong/placeholder vendor_id to the API.
        val vendorId = vendor.id
        if (vendorId == null) {
            _uiState.update { it.copy(errorMessage = "This order's vendor data is missing - can't save. Try reopening it.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            // GPS capture, not manual entry - same pattern as VendorFormViewModel.submit().
            val location = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
            val items = state.cartLines.map {
                OrderItemInput(it.product.id, it.quantity, it.sellPrice, it.discountPrice)
            }
            val result = editingOrderId?.let { orderRepository.update(it, vendorId, userId, items, location) }
                ?: orderRepository.create(vendorId, userId, items, location)

            when (result) {
                is NetworkResult.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.errors?.values?.firstOrNull()?.firstOrNull() ?: result.message,
                    )
                }
                is NetworkResult.Exception -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Couldn't reach the server. Check your connection and try again.")
                }
            }
        }
    }

    private fun updateLine(productId: Int, transform: (OrderCartLine) -> OrderCartLine) {
        _uiState.update { state ->
            state.copy(cartLines = state.cartLines.map { if (it.product.id == productId) transform(it) else it })
        }
    }

    private suspend fun searchVendor() {
        val query = _uiState.value.vendorQuery
        _uiState.update { it.copy(isSearchingVendor = true) }
        when (val result = vendorRepository.search(query, vendorLimit)) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(
                    vendorResults = result.data,
                    isSearchingVendor = false,
                    canLoadMoreVendors = vendorLimit < MAX_SEARCH_LIMIT && result.data.size >= vendorLimit,
                    errorMessage = null,
                )
            }
            is NetworkResult.Error -> _uiState.update {
                it.copy(vendorResults = emptyList(), isSearchingVendor = false, canLoadMoreVendors = false, errorMessage = result.message)
            }
            is NetworkResult.Exception -> _uiState.update {
                it.copy(
                    vendorResults = emptyList(),
                    isSearchingVendor = false,
                    canLoadMoreVendors = false,
                    errorMessage = "Couldn't search vendors. Check your connection and try again.",
                )
            }
        }
    }

    private suspend fun searchProduct() {
        val query = _uiState.value.productQuery
        if (query.isBlank()) {
            _uiState.update { it.copy(productResults = emptyList(), productResultsFromCache = false, canLoadMoreProducts = false) }
            return
        }
        _uiState.update { it.copy(isSearchingProduct = true) }
        when (val result = productRepository.search(query, productLimit)) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(
                    productResults = result.data,
                    productResultsFromCache = result.isFromCache,
                    isSearchingProduct = false,
                    canLoadMoreProducts = productLimit < MAX_SEARCH_LIMIT && result.data.size >= productLimit,
                    errorMessage = null,
                )
            }
            is NetworkResult.Error -> _uiState.update {
                it.copy(
                    productResults = emptyList(),
                    productResultsFromCache = false,
                    isSearchingProduct = false,
                    canLoadMoreProducts = false,
                    errorMessage = result.message,
                )
            }
            is NetworkResult.Exception -> _uiState.update {
                it.copy(
                    productResults = emptyList(),
                    productResultsFromCache = false,
                    isSearchingProduct = false,
                    canLoadMoreProducts = false,
                    errorMessage = "Couldn't search products. Check your connection and try again.",
                )
            }
        }
    }

    private companion object {
        const val SEARCH_LIMIT = 20
        const val MAX_SEARCH_LIMIT = 50
    }
}
