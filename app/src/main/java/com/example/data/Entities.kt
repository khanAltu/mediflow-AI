package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tenants",
    indices = [Index(value = ["slug"], unique = true)]
)
data class Tenant(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val status: String, // ACTIVE, TRIAL, SUSPENDED, DEACTIVATED
    val plan: String,   // FREE, BASIC, PRO, ENTERPRISE
    val subscriptionEnds: Long? // Null means indefinite, or timestamp of expiration
)

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true), Index(value = ["tenantId"])]
)
data class User(
    @PrimaryKey val id: String,
    val tenantId: String,
    val email: String,
    val name: String,
    val role: String, // SUPER_ADMIN, ADMIN, DOCTOR, STAFF
    val passwordHash: String
)

@Entity(
    tableName = "patients",
    indices = [Index(value = ["tenantId"])]
)
data class Patient(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val gender: String,
    val dob: String,
    val phone: String,
    val email: String
)

@Entity(
    tableName = "appointments",
    indices = [Index(value = ["tenantId"]), Index(value = ["doctorId", "startTime", "endTime"])]
)
data class Appointment(
    @PrimaryKey val id: String,
    val tenantId: String,
    val patientId: String,
    val doctorId: String,
    val startTime: Long,
    val endTime: Long,
    val status: String, // PENDING, CONFIRMED, CANCELLED, COMPLETED
    val reason: String
)

@Entity(
    tableName = "payments",
    indices = [Index(value = ["razorpayPaymentId"], unique = true), Index(value = ["tenantId"])]
)
data class Payment(
    @PrimaryKey val id: String,
    val razorpayPaymentId: String,
    val tenantId: String,
    val amount: Double,
    val status: String, // captured, failed
    val createdAt: Long
)
