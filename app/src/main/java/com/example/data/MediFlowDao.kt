package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediFlowDao {
    // --- TENANTS ---
    @Query("SELECT * FROM tenants")
    fun getAllTenants(): Flow<List<Tenant>>

    @Query("SELECT * FROM tenants WHERE id = :id LIMIT 1")
    suspend fun getTenantById(id: String): Tenant?

    @Query("SELECT * FROM tenants WHERE slug = :slug LIMIT 1")
    suspend fun getTenantBySlug(slug: String): Tenant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: Tenant)

    @Update
    suspend fun updateTenant(tenant: Tenant)

    // --- USERS ---
    @Query("SELECT * FROM users WHERE tenantId = :tenantId")
    fun getUsersForTenant(tenantId: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // --- PATIENTS ---
    @Query("SELECT * FROM patients WHERE tenantId = :tenantId ORDER BY name ASC")
    fun getPatientsForTenant(tenantId: String): Flow<List<Patient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient)

    @Delete
    suspend fun deletePatient(patient: Patient)

    // --- APPOINTMENTS ---
    @Query("SELECT * FROM appointments WHERE tenantId = :tenantId ORDER BY startTime ASC")
    fun getAppointmentsForTenant(tenantId: String): Flow<List<Appointment>>

    @Query("""
        SELECT * FROM appointments 
        WHERE tenantId = :tenantId 
          AND doctorId = :doctorId 
          AND status != 'CANCELLED'
          AND startTime < :endTime 
          AND endTime > :startTime
    """)
    suspend fun getOverlappingAppointments(
        tenantId: String,
        doctorId: String,
        startTime: Long,
        endTime: Long
    ): List<Appointment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment)

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)

    // --- PAYMENTS ---
    @Query("SELECT * FROM payments WHERE tenantId = :tenantId ORDER BY createdAt DESC")
    fun getPaymentsForTenant(tenantId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE razorpayPaymentId = :razorpayPaymentId LIMIT 1")
    suspend fun getPaymentByRazorpayId(razorpayPaymentId: String): Payment?

    @Insert(onConflict = OnConflictStrategy.ABORT) // Aborts if unique constraint fails, ensuring idempotency
    suspend fun insertPayment(payment: Payment)
}
