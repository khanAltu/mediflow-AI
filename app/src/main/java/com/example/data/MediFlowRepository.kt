package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class MediFlowRepository(private val dao: MediFlowDao) {

    // Seeding trigger to initialize default demo datasets
    suspend fun seedDatabaseIfEmpty() {
        // Only seed if no tenants exist
        val existingTenants = dao.getAllTenants().firstOrNull()
        if (!existingTenants.isNullOrEmpty()) return

        // 1. Initial Tenants
        val apolloTenant = Tenant(
            id = "tenant-apollo-id",
            name = "City Apollo Hospital",
            slug = "apollo-hospital",
            status = "TRIAL",
            plan = "BASIC",
            subscriptionEnds = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 7) // 7 days from now
        )
        val metroTenant = Tenant(
            id = "tenant-metro-id",
            name = "Metro Pediatric Clinic",
            slug = "metro-pediatric",
            status = "SUSPENDED",
            plan = "FREE",
            subscriptionEnds = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 2) // Expired 2 days ago
        )
        val alphaTenant = Tenant(
            id = "tenant-alpha-id",
            name = "Alpha Multi-Specialty",
            slug = "alpha-ms",
            status = "ACTIVE",
            plan = "PRO",
            subscriptionEnds = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30) // 30 days from now
        )

        dao.insertTenant(apolloTenant)
        dao.insertTenant(metroTenant)
        dao.insertTenant(alphaTenant)

        // 2. Initial Users
        val usersSeed = listOf(
            // Apollo Users
            User(
                id = "usr-apollo-admin",
                tenantId = "tenant-apollo-id",
                email = "admin@apollo.com",
                name = "Dr. Rajesh Kumar (Admin)",
                role = "ADMIN",
                passwordHash = "password123"
            ),
            User(
                id = "usr-apollo-doc1",
                tenantId = "tenant-apollo-id",
                email = "doctor.malhotra@apollo.com",
                name = "Dr. Sameer Malhotra",
                role = "DOCTOR",
                passwordHash = "doctor123"
            ),
            User(
                id = "usr-apollo-staff",
                tenantId = "tenant-apollo-id",
                email = "staff@apollo.com",
                name = "Priya Rao (Staff)",
                role = "STAFF",
                passwordHash = "staff123"
            ),
            // Metro Users
            User(
                id = "usr-metro-admin",
                tenantId = "tenant-metro-id",
                email = "admin@metro.com",
                name = "Dr. Sonal Sen",
                role = "ADMIN",
                passwordHash = "password123"
            ),
            User(
                id = "usr-metro-doc1",
                tenantId = "tenant-metro-id",
                email = "doc.verma@metro.com",
                name = "Dr. Sameer Verma",
                role = "DOCTOR",
                passwordHash = "doctor123"
            ),
            // Alpha Users
            User(
                id = "usr-alpha-admin",
                tenantId = "tenant-alpha-id",
                email = "admin@alpha.com",
                name = "Director Mehta",
                role = "ADMIN",
                passwordHash = "password123"
            ),
            User(
                id = "usr-alpha-doc",
                tenantId = "tenant-alpha-id",
                email = "doc@alpha.com",
                name = "Dr. Anirudh Bose",
                role = "DOCTOR",
                passwordHash = "doc123"
            )
        )

        for (user in usersSeed) {
            dao.insertUser(user)
        }

        // 3. Seed Patients
        val patientsSeed = listOf(
            Patient("pat-1", "tenant-apollo-id", "Aarav Sharma", "Male", "1988-12-04", "+91 98765 43210", "aarav@gmail.com"),
            Patient("pat-2", "tenant-apollo-id", "Kiara Advani", "Female", "1994-07-31", "+91 91234 56789", "kiara@gmail.com"),
            Patient("pat-3", "tenant-metro-id", "Reyansh Verma (Infant)", "Male", "2025-02-12", "+91 88888 77777", "parent.verma@gmail.com"),
            Patient("pat-4", "tenant-alpha-id", "Meera Joshi", "Female", "1975-09-14", "+91 77777 66666", "meera@gmail.com")
        )
        for (patient in patientsSeed) {
            dao.insertPatient(patient)
        }

        // 4. Seed Appointments
        val appointmentsSeed = listOf(
            Appointment(
                id = "app-1",
                tenantId = "tenant-apollo-id",
                patientId = "pat-1",
                doctorId = "usr-apollo-doc1",
                startTime = System.currentTimeMillis() + (1000L * 60 * 60 * 2), // in 2 hours
                endTime = System.currentTimeMillis() + (1000L * 60 * 60 * 3), // duration 1 hour
                status = "CONFIRMED",
                reason = "Routine Cardiovascular Consultation"
            )
        )
        for (app in appointmentsSeed) {
            dao.insertAppointment(app)
        }
    }

    // --- TENANTS METHODS ---
    fun getAllTenants(): Flow<List<Tenant>> = dao.getAllTenants()
    
    suspend fun getTenantById(id: String): Tenant? = dao.getTenantById(id)
    
    suspend fun getTenantBySlug(slug: String): Tenant? = dao.getTenantBySlug(slug)
    
    suspend fun registerTenant(tenantName: String, tenantSlug: String, adminEmail: String, adminName: String, adminPassHash: String): Tenant {
        val tenantId = "tenant-" + UUID.randomUUID().toString().take(6)
        val tenant = Tenant(
            id = tenantId,
            name = tenantName,
            slug = tenantSlug,
            status = "TRIAL",
            plan = "FREE",
            subscriptionEnds = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 7) // 7 days trial
        )
        
        val user = User(
            id = "usr-" + UUID.randomUUID().toString().take(6),
            tenantId = tenantId,
            email = adminEmail,
            name = adminName,
            role = "ADMIN",
            passwordHash = adminPassHash
        )

        dao.insertTenant(tenant)
        dao.insertUser(user)
        return tenant
    }

    suspend fun updateTenant(tenant: Tenant) {
        dao.updateTenant(tenant)
    }

    // --- USERS ---
    fun getUsersForTenant(tenantId: String): Flow<List<User>> = dao.getUsersForTenant(tenantId)
    
    suspend fun getUserByEmail(email: String): User? = dao.getUserByEmail(email)
    
    suspend fun getUserById(id: String): User? = dao.getUserById(id)

    suspend fun registerDoctorOrStaff(tenantId: String, email: String, name: String, role: String, pass: String): User {
        val user = User(
            id = "usr-" + UUID.randomUUID().toString().take(6),
            tenantId = tenantId,
            email = email,
            name = name,
            role = role,
            passwordHash = pass
        )
        dao.insertUser(user)
        return user
    }

    // --- PATIENTS ---
    fun getPatientsForTenant(tenantId: String): Flow<List<Patient>> = dao.getPatientsForTenant(tenantId)
    
    suspend fun insertPatient(patient: Patient) = dao.insertPatient(patient)
    
    suspend fun deletePatient(patient: Patient) = dao.deletePatient(patient)

    // --- APPOINTMENTS ---
    fun getAppointmentsForTenant(tenantId: String): Flow<List<Appointment>> = dao.getAppointmentsForTenant(tenantId)
    
    suspend fun insertAppointment(tenantId: String, appointment: Appointment): AppointmentResult {
        // First check overlap for the doctor
        if (appointment.endTime <= appointment.startTime) {
            return AppointmentResult.Error("Clinically invalid timeline: End time must occur after start time.")
        }
        
        val overlaps = dao.getOverlappingAppointments(
            tenantId = tenantId,
            doctorId = appointment.doctorId,
            startTime = appointment.startTime,
            endTime = appointment.endTime
        )
        
        if (overlaps.isNotEmpty()) {
            return AppointmentResult.Error(
                "Double-Booking Conflict: Doctor is already scheduled during this exact timeframe."
            )
        }
        
        dao.insertAppointment(appointment)
        return AppointmentResult.Success(appointment)
    }

    suspend fun updateAppointment(appointment: Appointment) {
        dao.updateAppointment(appointment)
    }

    suspend fun deleteAppointment(appointment: Appointment) = dao.deleteAppointment(appointment)

    // --- PAYMENTS & SIMULATED RAZORPAY WEBHOOK HANDLER ---
    fun getPaymentsForTenant(tenantId: String): Flow<List<Payment>> = dao.getPaymentsForTenant(tenantId)

    /**
     * Replicates the exact production logic discussed for the Razorpay Webhook Handler:
     * 1. Idempotency check on razorpayPaymentId (fails gracefully or returns processed log)
     * 2. Atomic addition of the Payment record
     * 3. Calculates cumulative validity extension (+30 days from either current expires or now)
     * 4. Updates Tenant status to ACTIVE and upgrades plan to PRO.
     */
    suspend fun simulateRazorpayWebhookPaymentCaptured(
        razorpayPaymentId: String,
        tenantId: String,
        amountPaid: Double
    ): WebhookResult {
        // 1. Idempotency Check
        val existingPayment = dao.getPaymentByRazorpayId(razorpayPaymentId)
        if (existingPayment != null) {
            return WebhookResult.Duplicate("Payment [$razorpayPaymentId] was already processed successfully. Idempotency Guard triggered inside SQLite transition.")
        }

        // 2. Fetch Active Tenant
        val tenant = dao.getTenantById(tenantId) 
            ?: return WebhookResult.Error("Target Tenant ID [$tenantId] not found associated with this Razorpay order metadata context.")

        // 3. Atomically Insert Payment and Update Tenant
        try {
            val paymentRecord = Payment(
                id = "pay-" + UUID.randomUUID().toString().take(6),
                razorpayPaymentId = razorpayPaymentId,
                tenantId = tenantId,
                amount = amountPaid,
                status = "captured",
                createdAt = System.currentTimeMillis()
            )

            // Insert matching payment trace
            dao.insertPayment(paymentRecord)

            // 4. Calculate Cumulative Validity
            val baseTime = if (tenant.subscriptionEnds != null && tenant.subscriptionEnds > System.currentTimeMillis()) {
                tenant.subscriptionEnds
            } else {
                System.currentTimeMillis()
            }

            // Expiry = Base + 30 days
            val extensionPeriod = 1000L * 60 * 60 * 24 * 30 // 30 days
            val newSubscriptionEnds = baseTime + extensionPeriod

            // Perform SQL entity updates
            val updatedTenant = tenant.copy(
                status = "ACTIVE",
                plan = "PRO",
                subscriptionEnds = newSubscriptionEnds
            )
            dao.updateTenant(updatedTenant)

            return WebhookResult.Success(
                payment = paymentRecord,
                newExpiryTimestamp = newSubscriptionEnds
            )
        } catch (e: Exception) {
            return WebhookResult.Error("Database transaction crash during payment reconciliation: ${e.message}")
        }
    }
}

sealed class AppointmentResult {
    data class Success(val appointment: Appointment): AppointmentResult()
    data class Error(val message: String): AppointmentResult()
}

sealed class WebhookResult {
    data class Success(val payment: Payment, val newExpiryTimestamp: Long): WebhookResult()
    data class Duplicate(val warning: String): WebhookResult()
    data class Error(val error: String): WebhookResult()
}
