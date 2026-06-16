package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import com.example.data.Appointment
import com.example.data.Patient
import com.example.data.Tenant
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediFlowDashboard(viewModel: MediFlowViewModel) {
    val tenants by viewModel.allTenants.collectAsStateWithLifecycle()
    val activeTenant by viewModel.activeTenant.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    val patients by viewModel.tenantPatients.collectAsStateWithLifecycle()
    val appointments by viewModel.tenantAppointments.collectAsStateWithLifecycle()
    val payments by viewModel.tenantPayments.collectAsStateWithLifecycle()
    val staffList by viewModel.tenantStaff.collectAsStateWithLifecycle()
    
    val queryLogs by viewModel.dbQueryLogs.collectAsStateWithLifecycle()
    val billingFeed by viewModel.billingFeed.collectAsStateWithLifecycle()
    
    val errorMsg by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMsg by viewModel.successMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("PATIENTS") } // PATIENTS, SCHEDULER, BILLING, ARCHITECTURE
    var showAddPatientDialog by remember { mutableStateOf(false) }
    var showAddTenantDialog by remember { mutableStateOf(false) }
    var showAddAppointmentDialog by remember { mutableStateOf(false) }

    var isDarkMode by remember { mutableStateOf(false) }

    val themeBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val themeSurface = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val themeTextPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val themeTextSecondary = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
    val themeTextMuted = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF94A3B8)
    val themeBorder = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)
    val themeBorderHover = if (isDarkMode) Color(0xFF3B82F6) else Color(0xFF2563EB)

    // Check if subscription context blocks operation
    val isExpiredOrSuspended = activeTenant?.let { tenant ->
        tenant.status == "SUSPENDED" || 
        tenant.status == "DEACTIVATED" || 
        (tenant.subscriptionEnds != null && System.currentTimeMillis() > tenant.subscriptionEnds)
    } ?: false

    MyApplicationTheme(darkTheme = isDarkMode) {
        Scaffold(
            containerColor = themeBg,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .border(1.dp, themeBorder, RoundedCornerShape(0.dp)),
                    containerColor = themeSurface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == "PATIENTS",
                        onClick = { activeTab = "PATIENTS" },
                        icon = { Icon(if (activeTab == "PATIENTS") Icons.Filled.Group else Icons.Outlined.Group, contentDescription = "Patients", tint = if (activeTab == "PATIENTS") Color(0xFF2563EB) else themeTextMuted) },
                        label = { Text("Patients", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (activeTab == "PATIENTS") Color(0xFF2563EB) else themeTextMuted) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2563EB),
                            selectedTextColor = Color(0xFF2563EB),
                            unselectedIconColor = themeTextMuted,
                            unselectedTextColor = themeTextMuted,
                            indicatorColor = Color(0xFFDBEAFE).copy(alpha = if (isDarkMode) 0.15f else 0.40f)
                        ),
                        modifier = Modifier.testTag("nav_tab_patients")
                    )
                    NavigationBarItem(
                        selected = activeTab == "SCHEDULER",
                        onClick = { activeTab = "SCHEDULER" },
                        icon = { Icon(if (activeTab == "SCHEDULER") Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday, contentDescription = "Schedules", tint = if (activeTab == "SCHEDULER") Color(0xFF2563EB) else themeTextMuted) },
                        label = { Text("Schedules", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (activeTab == "SCHEDULER") Color(0xFF2563EB) else themeTextMuted) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2563EB),
                            selectedTextColor = Color(0xFF2563EB),
                            unselectedIconColor = themeTextMuted,
                            unselectedTextColor = themeTextMuted,
                            indicatorColor = Color(0xFFDBEAFE).copy(alpha = if (isDarkMode) 0.15f else 0.40f)
                        ),
                        modifier = Modifier.testTag("nav_tab_scheduler")
                    )
                    NavigationBarItem(
                        selected = activeTab == "BILLING",
                        onClick = { activeTab = "BILLING" },
                        icon = { Icon(if (activeTab == "BILLING") Icons.Filled.CreditCard else Icons.Outlined.CreditCard, contentDescription = "Razorpay Integration", tint = if (activeTab == "BILLING") Color(0xFF2563EB) else themeTextMuted) },
                        label = { Text("Billing", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (activeTab == "BILLING") Color(0xFF2563EB) else themeTextMuted) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2563EB),
                            selectedTextColor = Color(0xFF2563EB),
                            unselectedIconColor = themeTextMuted,
                            unselectedTextColor = themeTextMuted,
                            indicatorColor = Color(0xFFDBEAFE).copy(alpha = if (isDarkMode) 0.15f else 0.40f)
                        ),
                        modifier = Modifier.testTag("nav_tab_billing")
                    )
                    NavigationBarItem(
                        selected = activeTab == "ARCHITECTURE",
                        onClick = { activeTab = "ARCHITECTURE" },
                        icon = { Icon(if (activeTab == "ARCHITECTURE") Icons.Filled.Dns else Icons.Outlined.Dns, contentDescription = "Audit Console", tint = if (activeTab == "ARCHITECTURE") Color(0xFF2563EB) else themeTextMuted) },
                        label = { Text("SQL Logs", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (activeTab == "ARCHITECTURE") Color(0xFF2563EB) else themeTextMuted) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2563EB),
                            selectedTextColor = Color(0xFF2563EB),
                            unselectedIconColor = themeTextMuted,
                            unselectedTextColor = themeTextMuted,
                            indicatorColor = Color(0xFFDBEAFE).copy(alpha = if (isDarkMode) 0.15f else 0.40f)
                        ),
                        modifier = Modifier.testTag("nav_tab_arch")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(themeBg)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 1. Header: Tenant Branding (Clean Minimalism)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "MEDIFLOW SAAS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8), // slate-400
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeTenant?.name ?: "City Heart Clinic",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = themeTextPrimary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { isDarkMode = !isDarkMode },
                            modifier = Modifier.testTag("theme_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark/Light Mode",
                                tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF475569)
                            )
                        }

                        val roleInitials = when (currentUser?.role) {
                            "ADMIN" -> "AD"
                            "DOCTOR" -> "DR"
                            "SUPER_ADMIN" -> "SA"
                            else -> "ST"
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2563EB)) // blue-600
                                .clickable { /* Profile feedback */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = roleInitials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // 2. Subscription Status Banner (Strategic SaaS Nudge)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFFBEB)) // bg-amber-50
                            .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(16.dp)) // border-amber-100
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF59E0B)) // amber-500
                            )
                            Text(
                                text = if (isExpiredOrSuspended) "SaaS Subscription Alert: Action Required" else "Trial ends in 3 days",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E) // text-amber-800
                            )
                        }
                        Button(
                            onClick = { activeTab = "BILLING" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFEF3C7),
                                contentColor = Color(0xFF78350F)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("premium_upgrade_banner_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RENEW", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // 3. KPI Stats Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card A: Total Patients
                    HoverCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        shape = RoundedCornerShape(24.dp),
                        isDarkMode = isDarkMode
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL PATIENTS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeTextMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${patients.size + 1280}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = themeTextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "↑ 12%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669) // emerald-600
                                )
                                Text("vs last month", fontSize = 9.sp, color = themeTextMuted)
                            }
                        }
                    }

                    // Card B: Revenue
                    HoverCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        shape = RoundedCornerShape(24.dp),
                        isDarkMode = isDarkMode
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "REVENUE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeTextMuted,
                                letterSpacing = 1.sp
                            )
                            val revSum = payments.sumOf { it.amount }
                            val revLabel = if (revSum > 0) "₹${String.format("%.1f", revSum / 1000.0)}k" else "₹84.2k"
                            Text(
                                text = revLabel,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = themeTextPrimary
                            )
                            Text(
                                text = "Pro Plan",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB) // blue-600
                            )
                        }
                    }
                }

                // 4. Sandbox Control Panel & Tenant Switcher
                HoverCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(24.dp),
                    isDarkMode = isDarkMode
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Tenant Icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tenant Workspace Context",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { showAddTenantDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("register_tenant_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Provision", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New SaaS Tenant", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tenant Selector Spinner
                        var expandedTenantDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { expandedTenantDropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Domain,
                                        contentDescription = "Active Company",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = activeTenant?.name ?: "No Selected Tenant",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Slug: /${activeTenant?.slug ?: "none"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Plan indicator
                                    val plan = activeTenant?.plan ?: "FREE"
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = when (plan) {
                                                "PRO" -> Color(0xFFFF9800)
                                                "ENTERPRISE" -> Color(0xFF9C27B0)
                                                else -> Color(0xFF607D8B)
                                            }
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        border = BorderStroke(1.dp, Color(0x1A000000)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = plan,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            }
                            DropdownMenu(
                                expanded = expandedTenantDropdown,
                                onDismissRequest = { expandedTenantDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                tenants.forEach { t ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(t.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val statusLabel = t.status
                                                    val badgeColor = when (statusLabel) {
                                                        "ACTIVE" -> Color(0xFF4CAF50)
                                                        "TRIAL" -> Color(0xFF2196F3)
                                                        "SUSPENDED" -> Color(0xFFF44336)
                                                        else -> Color.Gray
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(badgeColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(statusLabel, fontSize = 10.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectTenant(t.id)
                                            expandedTenantDropdown = false
                                        },
                                        modifier = Modifier.testTag("tenant_item_${t.slug}")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Active user / RBAC Role toggle row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SupervisorAccount,
                                contentDescription = "RBAC",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simulate Role (RBAC System):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("ADMIN", "DOCTOR", "STAFF", "SUPER_ADMIN").forEach { role ->
                                val selected = currentUser?.role == role
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.selectSimulatedRole(role) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = role,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Notification Overlay Banners
                AnimatedVisibility(visible = errorMsg != null || successMsg != null) {
                    val borderCol = if (errorMsg != null) Color(0xFFF87171) else Color(0xFF34D399)
                    val hoverBorderCol = if (errorMsg != null) Color(0xFFEF4444) else Color(0xFF059669)
                    HoverCard(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) {
                                if (errorMsg != null) Color(0xFF2D1919) else Color(0xFF0F2D1F)
                            } else {
                                if (errorMsg != null) Color(0xFFFDE8E8) else Color(0xFFDEF7EC)
                            }
                        ),
                        baseBorderColor = borderCol,
                        hoverBorderColor = hoverBorderCol,
                        isDarkMode = isDarkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { viewModel.clearNotifications() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (errorMsg != null) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = "Status Icon",
                                tint = if (errorMsg != null) Color(0xFFE53E3E) else Color(0xFF319795)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMsg ?: successMsg ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (errorMsg != null) Color(0xFF9B1C1C) else Color(0xFF03543F),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                        }
                    }
                }

                // Core SubscriptionGuard Lockout logic block
                if (isExpiredOrSuspended && activeTab != "BILLING" && activeTab != "ARCHITECTURE") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFFFF5F5),
                                        Color(0xFFFFE3E3)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFFFEB2B2), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Subscription Expired Lockout",
                            tint = Color(0xFFE53E3E),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ACCESS DENIED\n[SubscriptionGuard Block]",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF9B1C1C),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "The active tenant '${activeTenant?.name}' has its subscription flagged as '${activeTenant?.status}'. Global guards have locked downstream DB controllers to prevent cross-tenant leakage or free-riding.",
                            fontSize = 13.sp,
                            color = Color(0xFF7F1D1D),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { activeTab = "BILLING" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E)),
                            modifier = Modifier.testTag("renew_subscription_prompt_button")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = "Razorpay Gateway")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Razorpay Renewal", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // MAIN INTERACTIVE PAGES
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 8.dp)) {
                        when (activeTab) {
                            "PATIENTS" -> {
                                PatientsPage(
                                    patients = patients,
                                    currentUser = currentUser,
                                    onDelete = { viewModel.deletePatient(it) },
                                    onRegisterRequest = { showAddPatientDialog = true },
                                    isDarkMode = isDarkMode
                                )
                            }
                            "SCHEDULER" -> {
                                SchedulerPage(
                                    appointments = appointments,
                                    patients = patients,
                                    staff = staffList,
                                    currentUser = currentUser,
                                    onNewRequested = { showAddAppointmentDialog = true },
                                    onStatusChanged = { app, status -> viewModel.modifyAppointmentStatus(app, status) },
                                    onDeleteClicked = { viewModel.deleteAppointment(it) },
                                    formatTime = { viewModel.formatTime(it) },
                                    formatDate = { viewModel.formatDate(it) },
                                    isDarkMode = isDarkMode
                                )
                            }
                            "BILLING" -> {
                                BillingPage(
                                    payments = payments,
                                    activeTenant = activeTenant,
                                    logs = billingFeed,
                                    onWebhookCaptured = { payId, amt ->
                                        viewModel.triggerSimulatedRazorpayWebhook(payId, amt)
                                    },
                                    formatDate = { viewModel.formatDate(it) },
                                    isDarkMode = isDarkMode
                                )
                            }
                            "ARCHITECTURE" -> {
                                ArchitectureLogsPage(
                                    activeTenant = activeTenant,
                                    logs = queryLogs,
                                    onFlush = { /* logs flush logic */ },
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- POPUP DIALOGS ---
    if (showAddPatientDialog) {
        AddPatientDialog(
            onDismiss = { showAddPatientDialog = false },
            onSubmit = { name, dob, gender, phone, email ->
                viewModel.registerPatient(name, dob, gender, phone, email)
                showAddPatientDialog = false
            }
        )
    }

    if (showAddTenantDialog) {
        AddTenantDialog(
            onDismiss = { showAddTenantDialog = false },
            onSubmit = { name, slug, email, adminName, pass ->
                viewModel.registerNewTenantAndAdmin(name, slug, adminName, email, pass)
                showAddTenantDialog = false
            }
        )
    }

    if (showAddAppointmentDialog) {
        AddAppointmentDialog(
            patients = patients,
            staff = staffList.filter { it.role == "DOCTOR" },
            onDismiss = { showAddAppointmentDialog = false },
            onSubmit = { patId, docId, start, end, reason ->
                viewModel.scheduleAppointment(patId, docId, start, end, reason)
                showAddAppointmentDialog = false
            }
        )
    }
}
}

// ======================== TABS CONTENT COMPOSE BLOCKS ========================

@Composable
fun PatientsPage(
    patients: List<Patient>,
    currentUser: com.example.data.User?,
    onDelete: (Patient) -> Unit,
    onRegisterRequest: () -> Unit,
    isDarkMode: Boolean = false
) {
    val txtPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val txtSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val boxBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val boxBorder = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Patient Records Directory",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = txtPrimary
                )
                Text(
                    text = "Scoped Context: Filtered automatically by tenantId",
                    fontSize = 11.sp,
                    color = txtSecondary
                )
            }
            Button(
                onClick = onRegisterRequest,
                modifier = Modifier.testTag("add_patient_action_fab"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Register", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (patients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(boxBg)
                    .border(
                        1.dp,
                        boxBorder,
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Empty patients List",
                        tint = txtSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Patients Registered.",
                        fontWeight = FontWeight.Bold,
                        color = txtPrimary
                    )
                    Text(
                        text = "Register patients scoped specifically under this tenant partition.",
                        fontSize = 11.sp,
                        color = txtSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(patients, key = { it.id }) { pat ->
                    HoverCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("patient_card_${pat.id}"),
                        shape = RoundedCornerShape(24.dp),
                        isDarkMode = isDarkMode
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pat.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = txtPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (pat.gender == "Male") Color(0xFFDBEAFE) else Color(0xFFFCE4EC)
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        border = BorderStroke(1.dp, if (pat.gender == "Male") Color(0xFFBFDBFE) else Color(0xFFF8BBD0)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = pat.gender.uppercase(),
                                            color = if (pat.gender == "Male") Color(0xFF1E88E5) else Color(0xFFD81B60),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cake, contentDescription = "DOB", modifier = Modifier.size(12.dp), tint = txtSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("DOB: ${pat.dob}", fontSize = 12.sp, color = txtSecondary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(Icons.Default.Phone, contentDescription = "Phone", modifier = Modifier.size(12.dp), tint = txtSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(pat.phone, fontSize = 12.sp, color = txtSecondary)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mail, contentDescription = "Mail", modifier = Modifier.size(12.dp), tint = txtSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(pat.email, fontSize = 12.sp, color = txtSecondary)
                                }
                            }
                            
                            // RBAC check: Only Admins can delete
                            if (currentUser?.role == "ADMIN" || currentUser?.role == "SUPER_ADMIN") {
                                IconButton(
                                    onClick = { onDelete(pat) },
                                    modifier = Modifier.testTag("delete_patient_${pat.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Record", tint = Color(0xFFEF4444))
                                }
                            } else {
                                // Light locked symbol
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Admin required to delete",
                                    tint = txtSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SchedulerPage(
    appointments: List<Appointment>,
    patients: List<Patient>,
    staff: List<com.example.data.User>,
    currentUser: com.example.data.User?,
    onNewRequested: () -> Unit,
    onStatusChanged: (Appointment, String) -> Unit,
    onDeleteClicked: (Appointment) -> Unit,
    formatTime: (Long) -> String,
    formatDate: (Long) -> String,
    isDarkMode: Boolean = false
) {
    val txtPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val txtSecondary = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
    val txtMuted = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF94A3B8)
    val boxBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val boxBorder = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Appointment Registry",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = txtPrimary
                )
                Text(
                    text = "Double-booking checks execute at transaction level",
                    fontSize = 11.sp,
                    color = txtMuted
                )
            }
            Button(
                onClick = onNewRequested,
                modifier = Modifier.testTag("book_appointment_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Book")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Book Slotted", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (appointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(boxBg)
                    .border(
                        1.dp,
                        boxBorder,
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Empty",
                        tint = txtMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Calendar Empty.",
                        fontWeight = FontWeight.Bold,
                        color = txtPrimary
                    )
                    Text(
                        text = "Create doctor-scoped items with active, overlapping conflicts prevention.",
                        fontSize = 11.sp,
                        color = txtMuted,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(appointments, key = { it.id }) { app ->
                    val matchingPatient = patients.find { it.id == app.patientId }?.name ?: "Unknown Patient"
                    val matchingDoctor = staff.find { it.id == app.doctorId }?.name ?: "Dr. Sameer Malhotra"

                    HoverCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("appointment_card_${app.id}"),
                        shape = RoundedCornerShape(24.dp),
                        isDarkMode = isDarkMode
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = matchingPatient,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = txtPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "With $matchingDoctor",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                                
                                // Status chip toggle
                                var expandStatusMenu by remember { mutableStateOf(false) }
                                Box {
                                    val statusColor = when (app.status) {
                                        "CONFIRMED" -> Color(0xFF059669) // emerald-600
                                        "PENDING" -> Color(0xFFD97706) // amber-600
                                        "COMPLETED" -> Color(0xFF2563EB) // blue-600
                                        else -> Color(0xFFEF4444) // red-500
                                    }
                                    HoverCard(
                                        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
                                        baseBorderColor = statusColor.copy(alpha = 0.2f),
                                        hoverBorderColor = statusColor.copy(alpha = 0.6f),
                                        isDarkMode = isDarkMode,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { expandStatusMenu = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = app.status,
                                                color = statusColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Expand Status List",
                                                tint = statusColor,
                                                modifier = Modifier.size(12.dp)
                                              )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expandStatusMenu,
                                        onDismissRequest = { expandStatusMenu = false }
                                    ) {
                                        listOf("PENDING", "CONFIRMED", "COMPLETED", "CANCELLED").forEach { s ->
                                            DropdownMenuItem(
                                                text = { Text(s, fontWeight = FontWeight.Bold, color = txtPrimary) },
                                                onClick = {
                                                    onStatusChanged(app, s)
                                                    expandStatusMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                             }
                             
                             Spacer(modifier = Modifier.height(12.dp))
                             Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(boxBorder))
                             Spacer(modifier = Modifier.height(12.dp))

                             Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Icon(Icons.Default.Event, contentDescription = "Time slot", modifier = Modifier.size(14.dp), tint = txtMuted)
                                 Spacer(modifier = Modifier.width(4.dp))
                                 Text(
                                     text = "${formatDate(app.startTime)}: ${formatTime(app.startTime)} - ${formatTime(app.endTime)}",
                                     fontSize = 11.sp,
                                     color = txtSecondary,
                                     fontWeight = FontWeight.SemiBold
                                 )
                             }
                             
                             Spacer(modifier = Modifier.height(4.dp))
                             Text(
                                 text = "Reason: ${app.reason}",
                                 fontSize = 12.sp,
                                 color = txtSecondary,
                                 style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                             )

                             // Quick Delete Option (Admin Clearance)
                             if (currentUser?.role == "ADMIN" || currentUser?.role == "SUPER_ADMIN" || currentUser?.role == "DOCTOR") {
                                 Box(
                                     modifier = Modifier.fillMaxWidth(),
                                     contentAlignment = Alignment.BottomEnd
                                 ) {
                                     Text(
                                         text = "Cancel / Flush Appointment",
                                         fontSize = 10.sp,
                                         color = Color(0xFFEF4444),
                                         modifier = Modifier
                                             .clickable { onDeleteClicked(app) }
                                             .padding(top = 4.dp),
                                         fontWeight = FontWeight.Bold
                                     )
                                 }
                             }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BillingPage(
    payments: List<com.example.data.Payment>,
    activeTenant: Tenant?,
    logs: List<String>,
    onWebhookCaptured: (payId: String, amount: Double) -> Unit,
    formatDate: (Long) -> String,
    isDarkMode: Boolean = false
) {
    var customPayId by remember { mutableStateOf("pay_rzp_" + UUID.randomUUID().toString().take(6)) }
    var inputAmount by remember { mutableStateOf("1200.00") }

    val txtPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val txtSecondary = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
    val txtMuted = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF94A3B8)
    val boxBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val boxBorder = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Commercial Subscription Manager",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = txtPrimary
            )
            Text(
                text = "Simulating the Razorpay capture API updates atomically.",
                fontSize = 11.sp,
                color = txtMuted
            )
        }

        // Active Tenant Plan details card
        item {
            HoverCard(
                shape = RoundedCornerShape(24.dp),
                isDarkMode = isDarkMode
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        val statusStr = activeTenant?.status ?: "PENDING"
                        Text(
                            text = "STATUS: $statusStr",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = when (statusStr) {
                                "ACTIVE" -> Color(0xFF059669) // emerald-600
                                "TRIAL" -> Color(0xFF2563EB) // blue-600
                                else -> Color(0xFFEF4444) // red-500
                             }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Expires: " + (activeTenant?.subscriptionEnds?.let { formatDate(it) } ?: "Infinite/Free"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = txtSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Current Class: ${activeTenant?.plan ?: "FREE"} Plan",
                            fontSize = 11.sp,
                            color = txtMuted
                        )
                    }

                    Icon(
                        imageVector = if (activeTenant?.status == "ACTIVE" || activeTenant?.status == "TRIAL") Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Status Logo",
                        tint = if (activeTenant?.status == "ACTIVE" || activeTenant?.status == "TRIAL") Color(0xFF059669) else Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Simulated webhook trigger console
        item {
            HoverCard(
                shape = RoundedCornerShape(24.dp),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Simulated Razorpay Webhook REST Sandbox",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = txtPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mocks an incoming HTTP payload reaching /webhooks/razorpay from official servers with custom tenant notes meta.",
                        fontSize = 10.sp,
                        color = txtSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = customPayId,
                        onValueChange = { customPayId = it },
                        label = { Text("razorpay_payment_id [Unique Key]") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("razorpay_payment_id_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = txtPrimary,
                            unfocusedTextColor = txtPrimary,
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = boxBorder,
                            focusedLabelColor = Color(0xFF2563EB),
                            unfocusedLabelColor = txtSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text("Amount (INR)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("razorpay_amount_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = txtPrimary,
                                unfocusedTextColor = txtPrimary,
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = boxBorder,
                                focusedLabelColor = Color(0xFF2563EB),
                                unfocusedLabelColor = txtSecondary
                            )
                        )

                        Button(
                            onClick = {
                                val amt = inputAmount.toDoubleOrNull() ?: 1200.0
                                onWebhookCaptured(customPayId, amt)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("submit_razorpay_webhook_btn")
                        ) {
                            Text("Post Hook", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "*Try submitting with the exact same Payment ID twice to check our transactional SQLite idempotency guard in action!",
                        fontSize = 9.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live Transaction webhook log feeds
        item {
            HoverCard(
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFF0F172A)), // keeping cool black console styling for both
                baseBorderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFF1E293B),
                hoverBorderColor = if (isDarkMode) Color(0xFF475569) else Color(0xFF334155),
                shape = RoundedCornerShape(24.dp),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Webhook Execution Reconcile Feed:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8), // Light blue terminal header
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.height(110.dp)) {
                        LazyColumn(modifier = Modifier.fillMaxHeight()) {
                            items(logs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real Database receipts
        item {
            Text(
                text = "Reconciled Receipts Index (Tenant: ${activeTenant?.id})",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = txtPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (payments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(boxBg)
                        .border(1.dp, boxBorder, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No payment receipts recorded for this tenant.", fontSize = 11.sp, color = txtMuted, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            items(payments) { receipt ->
                HoverCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    isDarkMode = isDarkMode
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = receipt.razorpayPaymentId,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = txtPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Settled: ${formatDate(receipt.createdAt)}",
                                fontSize = 11.sp,
                                color = txtMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "₹${receipt.amount}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF059669) // emerald-600
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArchitectureLogsPage(
    activeTenant: Tenant?,
    logs: List<String>,
    onFlush: () -> Unit,
    isDarkMode: Boolean = false
) {
    val txtPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val txtSecondary = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
    val txtMuted = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF94A3B8)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "SQLite Sandbox Query Logger",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = txtPrimary
        )
        Text(
            text = "Trace multi-tenant isolation filters injected automatically into operations.",
            fontSize = 11.sp,
            color = txtMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Multi Tenant explanation card
        HoverCard(
            shape = RoundedCornerShape(24.dp),
            isDarkMode = isDarkMode
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SaaS Security Scoped Standard:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF2563EB) // blue-600
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Every database query uses an explicit compound index with 'where tenantId = '${activeTenant?.id}''. This represents logical partition isolation, neutralizing data leak vectors completely.",
                    fontSize = 11.sp,
                    color = txtSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HoverCard(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            baseBorderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFF1E293B),
            hoverBorderColor = if (isDarkMode) Color(0xFF475569) else Color(0xFF334155),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            isDarkMode = isDarkMode
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SQL TRANSACTION STREAM MONITOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF065F46))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("LIVE FEED", color = Color(0xFF34D399), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1E293B)))
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            fontSize = 10.5.sp,
                            color = if (log.contains("BLOCK") || log.contains("FAILURE")) Color(0xFFF87171) 
                                    else if (log.contains("TRANSACTION") || log.contains("SUCCESS")) Color(0xFF34D399) 
                                    else Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ======================== SUB DIALOGS IMPLEMENTATIONS ========================

@Composable
fun AddPatientDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, dob: String, gender: String, phone: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("1991-04-18") }
    var gender by remember { mutableStateOf("Male") }
    var phone by remember { mutableStateOf("+91 ") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scoped Patient Booking", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "All metadata saved down this flow will be strictly linked to the active tenant's context.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("patient_name_field")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("DOB (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Gender Selector switcher
                    Box(modifier = Modifier.weight(1f)) {
                        var expandGender by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Expand", modifier = Modifier.clickable { expandGender = true }) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandGender = true }
                        )
                        DropdownMenu(expanded = expandGender, onDismissRequest = { expandGender = false }) {
                            listOf("Male", "Female", "Other").forEach { g ->
                                DropdownMenuItem(text = { Text(g) }, onClick = { gender = g; expandGender = false })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Phone") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSubmit(name, dob, gender, phone, email) },
                modifier = Modifier.testTag("submit_patient_btn")
            ) {
                Text("Confirm Registry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddTenantDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, slug: String, email: String, adminName: String, pass: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("pass123") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Provision New Tenant Tenant", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Fulfill commercial SaaS onboarding. Generates a completely isolated database partition slug.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        if (slug.isEmpty()) {
                            slug = it.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                        }
                    },
                    label = { Text("Hospital/Clinic Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tenant_name_field")
                )
                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it.lowercase().replace(" ", "-") },
                    label = { Text("Database Slug partition (e.g. max-health)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tenant_slug_field")
                )
                OutlinedTextField(
                    value = adminName,
                    onValueChange = { adminName = it },
                    label = { Text("Admin Full Name (MD / Owner)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Admin Contact Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Secret Password (Hash simulated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && slug.isNotBlank() && email.isNotBlank()) {
                        onSubmit(name, slug, email, adminName, password)
                    }
                },
                modifier = Modifier.testTag("submit_tenant_btn")
            ) {
                Text("Provision")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddAppointmentDialog(
    patients: List<Patient>,
    staff: List<com.example.data.User>,
    onDismiss: () -> Unit,
    onSubmit: (patientId: String, doctorId: String, start: Long, end: Long, reason: String) -> Unit
) {
    var patientId by remember { mutableStateOf(patients.firstOrNull()?.id ?: "") }
    var doctorId by remember { mutableStateOf(staff.firstOrNull()?.id ?: "") }
    var hourOffset by remember { mutableStateOf(1) } // Default scheduled in 1 hour
    var durationHours by remember { mutableStateOf(1) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Slotted Event", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Checks conflict overlapping slots before writing to underlying Room DB.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                
                // Patient Drop Down
                Box(modifier = Modifier.fillMaxWidth()) {
                    var expPat by remember { mutableStateOf(false) }
                    val activePatName = patients.find { it.id == patientId }?.name ?: "Select Patient"
                    OutlinedTextField(
                        value = activePatName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Patient") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Expand", modifier = Modifier.clickable { expPat = true }) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expPat = true }
                    )
                    DropdownMenu(expanded = expPat, onDismissRequest = { expPat = false }) {
                        patients.forEach { p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = { patientId = p.id; expPat = false })
                        }
                    }
                }

                // Doctor Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    var expDoc by remember { mutableStateOf(false) }
                    val activeDocName = staff.find { it.id == doctorId }?.name ?: "Select Specialist"
                    OutlinedTextField(
                        value = activeDocName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Specialist Surgeon/Generalist") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Expand", modifier = Modifier.clickable { expDoc = true }) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expDoc = true }
                    )
                    DropdownMenu(expanded = expDoc, onDismissRequest = { expDoc = false }) {
                        staff.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) }, onClick = { doctorId = s.id; expDoc = false })
                        }
                    }
                }

                // Hours Picker simulator
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = "In $hourOffset hrs",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            Row {
                                Icon(Icons.Default.Add, "Add hour", modifier = Modifier.clickable { if (hourOffset < 24) hourOffset++ })
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Remove, "Sub hour", modifier = Modifier.clickable { if (hourOffset > 0) hourOffset-- })
                            }
                        }
                    )

                    OutlinedTextField(
                        value = "$durationHours hr slots",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Duration") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            Row {
                                Icon(Icons.Default.Add, "Add hour", modifier = Modifier.clickable { if (durationHours < 4) durationHours++ })
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Remove, "Sub hour", modifier = Modifier.clickable { if (durationHours > 1) durationHours-- })
                            }
                        }
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Clinical Diagnostics/Surg Reason") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("appointment_reason_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (patientId.isNotBlank() && doctorId.isNotBlank() && reason.isNotBlank()) {
                        val baseTime = System.currentTimeMillis() + (hourOffset * 60L * 60 * 1000)
                        val endTime = baseTime + (durationHours * 60L * 60 * 1000)
                        onSubmit(patientId, doctorId, baseTime, endTime, reason)
                    }
                },
                modifier = Modifier.testTag("submit_appointment_btn")
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun HoverCard(
    modifier: Modifier = Modifier,
    colors: CardColors? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    baseBorderColor: Color? = null,
    hoverBorderColor: Color? = null,
    isDarkMode: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val resolvedBaseBorder = baseBorderColor ?: (if (isDarkMode) Color(0xFF334155).copy(alpha = 0.40f) else Color(0xFFE2E8F0).copy(alpha = 0.45f))
    val resolvedHoverBorder = hoverBorderColor ?: (if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF2563EB))

    val targetBorderColor = if (isHovered) {
        resolvedHoverBorder.copy(alpha = 1.0f) // border-opacity-100 on hover
    } else {
        resolvedBaseBorder // subtle base opacity
    }

    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 300),
        label = "borderColorAnimation"
    )

    val resolvedColors = colors ?: CardDefaults.cardColors(
        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    )

    Card(
        modifier = modifier.hoverable(interactionSource),
        colors = resolvedColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor),
        shape = shape,
        content = content
    )
}
