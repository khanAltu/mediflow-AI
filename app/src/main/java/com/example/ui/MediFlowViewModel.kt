package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MediFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MediFlowDatabase.getDatabase(application)
    private val repository = MediFlowRepository(database.dao())

    // --- SEED ENGINE ---
    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // --- SYSTEM CONTEXT STATES ---
    val allTenants: StateFlow<List<Tenant>> = repository.getAllTenants()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTenantId = MutableStateFlow<String>("tenant-apollo-id")
    val selectedTenantId: StateFlow<String> = _selectedTenantId.asStateFlow()

    val activeTenant: StateFlow<Tenant?> = combine(allTenants, selectedTenantId) { tenants, id ->
        tenants.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Simulated User
    private val _currentUser = MutableStateFlow<User?>(
        User(
            id = "usr-apollo-admin",
            tenantId = "tenant-apollo-id",
            email = "admin@apollo.com",
            name = "Dr. Rajesh Kumar",
            role = "ADMIN",
            passwordHash = "password123"
        )
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Scoped Data streams based on selected tenant
    @OptIn(ExperimentalCoroutinesApi::class)
    val tenantPatients: StateFlow<List<Patient>> = selectedTenantId
        .flatMapLatest { id -> repository.getPatientsForTenant(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tenantAppointments: StateFlow<List<Appointment>> = selectedTenantId
        .flatMapLatest { id -> repository.getAppointmentsForTenant(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tenantPayments: StateFlow<List<Payment>> = selectedTenantId
        .flatMapLatest { id -> repository.getPaymentsForTenant(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tenantStaff: StateFlow<List<User>> = selectedTenantId
        .flatMapLatest { id -> repository.getUsersForTenant(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UI INTERACTION feedback states ---
    private val _dbQueryLogs = MutableStateFlow<List<String>>(listOf("System Ready: Tenant filter initially set to [tenant-apollo-id]"))
    val dbQueryLogs: StateFlow<List<String>> = _dbQueryLogs.asStateFlow()

    private val _billingFeed = MutableStateFlow<List<String>>(listOf("Billing Ledger initiated. Awaiting Razorpay captured webhook triggers..."))
    val billingFeed: StateFlow<List<String>> = _billingFeed.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // --- LOGGING HELPER ---
    private fun addLog(message: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        _dbQueryLogs.update { listOf("[$timeStr] $message") + it.take(29) }
    }

    private fun addBillingLog(message: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        _billingFeed.update { listOf("[$timeStr] $message") + it.take(29) }
    }

    fun clearNotifications() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // --- TENANT SWAPPER & CONTEXT MIGRATOR ---
    fun selectTenant(tenantId: String) {
        _selectedTenantId.value = tenantId
        viewModelScope.launch {
            val t = repository.getTenantById(tenantId)
            addLog("CONTEXT RESOLUTION: SELECT * FROM tenants WHERE id = '$tenantId'; Status: ${t?.status}, Plan: ${t?.plan}")
            
            // Auto swap default user for this tenant
            val staffList = repository.getUsersForTenant(tenantId).first()
            val newDefaultUser = staffList.find { it.role == "ADMIN" } ?: staffList.firstOrNull()
            _currentUser.value = newDefaultUser
            addLog("RBAC CONTEXT RESOLUTION: Logged in as ${newDefaultUser?.name ?: "Unknown"} [Role: ${newDefaultUser?.role ?: "None"}] for Tenant ID [$tenantId]")
        }
    }

    // --- CUSTOM TENANT REGISTRATION (PHASE 1 CORE) ---
    fun registerNewTenantAndAdmin(
        tenantName: String,
        tenantSlug: String,
        adminName: String,
        adminEmail: String,
        adminPass: String
    ) {
        viewModelScope.launch {
            try {
                // Ensure unique slug check
                val existing = repository.getTenantBySlug(tenantSlug)
                if (existing != null) {
                    _errorMessage.value = "Tenant Slug '$tenantSlug' already exists in the Postgres/SQLite database index!"
                    return@launch
                }

                addLog("TRANSACTION INITIALIZATION: Creating Tenant [$tenantName] and Admin [$adminName] atomically.")
                val newTenant = repository.registerTenant(
                    tenantName = tenantName,
                    tenantSlug = tenantSlug,
                    adminName = adminName,
                    adminEmail = adminEmail,
                    adminPassHash = adminPass
                )

                addLog("TRANSACTION SUCCESS: Tenant: ${newTenant.id}, Admin: $adminName created automatically.")
                _successMessage.value = "SaaS Tenant ${newTenant.name} registered! Auto-logged into its Sandbox."
                
                // Immediately switch to the new Tenant
                selectTenant(newTenant.id)
            } catch (e: Exception) {
                _errorMessage.value = "Registration Transaction rolled back: ${e.message}"
                addLog("TRANSACTION FAILURE: Rollback executed.")
            }
        }
    }

    // --- ROLE SWAPPER ---
    fun selectSimulatedRole(role: String) {
        val user = _currentUser.value
        if (user != null) {
            val updatedUser = user.copy(role = role)
            _currentUser.value = updatedUser
            addLog("RBAC STATE OVERRIDE: Swapped simulated session identity to role: $role. Views will dynamically filter.")
        }
    }

    // --- ADD REGISTER PATIENT (PHASE 2 DATA SCOPING) ---
    fun registerPatient(name: String, dob: String, gender: String, phone: String, email: String) {
        val tenantId = _selectedTenantId.value
        val patientId = "pat-" + UUID.randomUUID().toString().take(6)
        
        viewModelScope.launch {
            // Guard enforcement check (Simulated SubscriptionGuard check)
            if (!isSubscriptionActive()) {
                addLog("GUARD BLOCK: [SubscriptionGuard] prevented INSERT INTO patients: Tenant is not ACTIVE.")
                _errorMessage.value = "Blocked by SubscriptionGuard. Active billing required down this code path."
                return@launch
            }

            // Guard Role authorization (simulates RBAC)
            val currentRole = _currentUser.value?.role
            if (currentRole == "DOCTOR" || currentRole == "STAFF" || currentRole == "ADMIN" || currentRole == "SUPER_ADMIN") {
                val newPatient = Patient(
                    id = patientId,
                    tenantId = tenantId,
                    name = name,
                    gender = gender,
                    dob = dob,
                    phone = phone,
                    email = if (email.isBlank()) "n/a" else email
                )
                repository.insertPatient(newPatient)
                addLog("SQL EXECUTION: INSERT INTO patients (id, tenantId, name) VALUES ('$patientId', '$tenantId', '$name') -- SCOPED QUERY SUCCESS")
                _successMessage.value = "Patient '$name' successfully added to $tenantId ledger."
            } else {
                _errorMessage.value = "Access Denied: Simulated Role [$currentRole] is unauthorized."
                addLog("RBAC GUARD FAIL: Role [$currentRole] is unauthorized to add patients.")
            }
        }
    }

    // --- REVENUE PRICING ESTIMATES ---
    fun deletePatient(patient: Patient) {
        viewModelScope.launch {
            if (!isSubscriptionActive()) {
                _errorMessage.value = "Subscription expired or suspended. Lockout is active."
                return@launch
            }
            // ADMIN, SUPER_ADMIN required for deletion (demo purposes)
            val currentRole = _currentUser.value?.role
            if (currentRole == "ADMIN" || currentRole == "SUPER_ADMIN") {
                repository.deletePatient(patient)
                addLog("SQL EXECUTION: DELETE FROM patients WHERE id = '${patient.id}' AND tenantId = '${patient.tenantId}'")
                _successMessage.value = "Patient record pruned."
            } else {
                _errorMessage.value = "RBAC privilege missing: Admin clearances required to delete medical records."
                addLog("RBAC BLOCKED: Privilege violation on patient deletion.")
            }
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch {
            repository.deleteAppointment(appointment)
            addLog("SQL EXECUTION: DELETE FROM appointments WHERE id = '${appointment.id}' AND tenantId = '${appointment.tenantId}'")
            _successMessage.value = "Appointment transaction pruned from calendar."
        }
    }

    // --- SCHEDULING WITH DOUBLE-BOOK PREVENTION (PHASE 3 SCHEDULER) ---
    fun scheduleAppointment(patientId: String, doctorId: String, startTimeMillis: Long, endTimeMillis: Long, reason: String) {
        val tenantId = _selectedTenantId.value
        val appId = "app-" + UUID.randomUUID().toString().take(6)

        viewModelScope.launch {
            if (!isSubscriptionActive()) {
                addLog("GUARD BLOCK: [SubscriptionGuard] blocked schedule creation.")
                _errorMessage.value = "Access suspended. Please renew subscription to manage schedules."
                return@launch
            }

            val app = Appointment(
                id = appId,
                tenantId = tenantId,
                patientId = patientId,
                doctorId = doctorId,
                startTime = startTimeMillis,
                endTime = endTimeMillis,
                status = "PENDING",
                reason = reason
            )

            addLog("SCHEDULER: Initiating overlap query for Doctor [$doctorId] between ${formatTime(startTimeMillis)} and ${formatTime(endTimeMillis)}")
            
            val result = repository.insertAppointment(tenantId, app)
            when (result) {
                is AppointmentResult.Success -> {
                    addLog("SQL EXECUTION & MERGE: INSERT INTO appointments SUCCESS. Dynamic schedule locked under $tenantId partition.")
                    _successMessage.value = "Appointment booked successfully with no booking conflicts!"
                }
                is AppointmentResult.Error -> {
                    addLog("SCHEDULING CLASH RESOLVED: Room blocked insert due to overlap criteria: ${result.message}")
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun modifyAppointmentStatus(app: Appointment, nextStatus: String) {
        viewModelScope.launch {
            val updated = app.copy(status = nextStatus)
            repository.updateAppointment(updated)
            addLog("SQL EXECUTION: UPDATE appointments SET status = '$nextStatus' WHERE id = '${app.id}' AND tenantId = '${app.tenantId}'")
            _successMessage.value = "Appointment updated to $nextStatus."
        }
    }

    // --- INTERACTIVE RAZORPAY WEBHOOK WORKSTATION ---
    fun triggerSimulatedRazorpayWebhook(customPaymentId: String, amount: Double) {
        val tenantId = _selectedTenantId.value
        
        viewModelScope.launch {
            addBillingLog("WEBHOOK RECEPTION: Webhook payload event [payment.captured] received via /webhooks/razorpay")
            addBillingLog("VERIFICATION PROCESS: computing HMAC-SHA256 signature against local webhook secret key.")
            
            // Simulating signature success (in a real server we check matching headers)
            addBillingLog("SIGNATURE CHECK: APPROVED cleanly! Reconciling order metadata notes: { tenantId: '$tenantId' }")
            
            val result = repository.simulateRazorpayWebhookPaymentCaptured(
                razorpayPaymentId = customPaymentId,
                tenantId = tenantId,
                amountPaid = amount
            )

            when (result) {
                is WebhookResult.Success -> {
                    addBillingLog("IDEMPOTENCY PASS: Payment reference '$customPaymentId' verified unique.")
                    addBillingLog("TRANSACTION COMMIT: Added $${amount} to audit logs. Tenant status upgraded to ACTIVE.")
                    addBillingLog("MEMBERSHIP EXTENSION: Subscription validity extended to ${formatDate(result.newExpiryTimestamp)}")
                    _successMessage.value = "Razorpay Webhook trigger absolute success! Account activated."
                    addLog("COMMERCIAL WEBHOOK RECONCILE: Upgrade tenant $tenantId status to ACTIVE.")
                }
                is WebhookResult.Duplicate -> {
                    addBillingLog("IDEMPOTENCY FAILURE DETECTED: ${result.warning}")
                    _errorMessage.value = "Webhook rejected: Duplicate Razorpay Payment ID detected to safeguard server from double processing!"
                }
                is WebhookResult.Error -> {
                    addBillingLog("WEBHOOK RECONCILE EXCEPTION: ${result.error}")
                    _errorMessage.value = result.error
                }
            }
        }
    }

    // --- AUX FUNCTIONS ---
    suspend fun isSubscriptionActive(): Boolean {
        val tenant = repository.getTenantById(_selectedTenantId.value) ?: return false
        if (tenant.status == "SUSPENDED" || tenant.status == "DEACTIVATED") {
            return false
        }
        val expires = tenant.subscriptionEnds
        if (expires != null && System.currentTimeMillis() > expires) {
            return false
        }
        return true
    }

    fun formatTime(timeMillis: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timeMillis))
    }

    fun formatDate(timeMillis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))
    }
}
