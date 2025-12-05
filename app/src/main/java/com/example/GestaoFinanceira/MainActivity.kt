package com.example.GestaoFinanceira

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GestaoFinanceira.ui.theme.AulaTelasTheme

// IMPORTAÇÕES DO FIREBASE E DATA
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// =======================================================
// CONFIGURAÇÕES E CORES
// =======================================================

object AppColors {
    val Primary = Color(0xFF1976D2)
    val Secondary = Color(0xFF616161)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color.White
    val Text = Color(0xFF212121)
    val Income = Color(0xFF388E3C)
    val Expense = Color(0xFFD32F2F)
}

enum class TransactionType { RECEITA, DESPESA }

// Modelo de dados (com valores padrão para o Firebase)
data class Transaction(
    val id: String = "",
    val description: String = "",
    val value: Double = 0.0,
    val type: TransactionType = TransactionType.DESPESA,
    val date: String = "",
    val category: String = ""
)

class BottomAppBarItem(val icon: ImageVector, val label: String)
class TopAppBarItem(var title: String)

sealed class ScreenItem(
    val topAppBarItem: TopAppBarItem,
    val bottomAppBarItem: BottomAppBarItem? = null
) {
    data object Dashboard : ScreenItem(
        TopAppBarItem("Painel Geral"),
        BottomAppBarItem(Icons.Default.Dashboard, "Painel")
    )

    data object Transactions : ScreenItem(
        TopAppBarItem("Histórico"),
        BottomAppBarItem(Icons.Default.List, "Histórico")
    )

    data object Settings : ScreenItem(
        TopAppBarItem("Configurações"),
        BottomAppBarItem(Icons.Default.Settings, "Ajustes")
    )
}

val mainScreens = listOf(ScreenItem.Dashboard, ScreenItem.Transactions, ScreenItem.Settings)

// =======================================================
// MAIN ACTIVITY
// =======================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AulaTelasTheme {
                MinimalTheme {
                    AuthWrapper()
                }
            }
        }
    }
}

@Composable
fun MinimalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.Primary,
            secondary = AppColors.Secondary,
            background = AppColors.Background,
            surface = AppColors.Surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = AppColors.Text,
            onSurface = AppColors.Text
        ),
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.Primary
            ),
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Text
            ),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = AppColors.Text
            )
        ),
        shapes = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp)
        ),
        content = content
    )
}

// =======================================================
// CONTROLE DE AUTENTICAÇÃO (WRAPPER)
// =======================================================

@Composable
fun AuthWrapper() {
    val auth = FirebaseAuth.getInstance()
    // Observa se existe usuário logado
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser != null) {
        // Usuário logado: Mostra o App
        App(
            userId = currentUser!!.uid,
            onLogout = { auth.signOut() }
        )
    } else {
        // Usuário deslogado: Mostra Login/Cadastro
        LoginScreen(
            onLoginSuccess = { /* O listener acima cuidará da navegação */ }
        )
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // Estados do formulário
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) } // true = Login, false = Cadastro
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Primary),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = if (isLoginMode) "Bem-vindo de volta" else "Crie sua conta",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Text,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AppColors.Primary)
                } else {
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true
                            if (isLoginMode) {
                                // LOGIN
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            onLoginSuccess()
                                        } else {
                                            Toast.makeText(context, "Erro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                // CADASTRO
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            onLoginSuccess()
                                        } else {
                                            Toast.makeText(context, "Erro ao criar conta: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) {
                        Text(if (isLoginMode) "ENTRAR" else "CADASTRAR")
                    }
                }

                TextButton(onClick = { isLoginMode = !isLoginMode }) {
                    Text(
                        text = if (isLoginMode) "Não tem conta? Cadastre-se" else "Já tem conta? Faça login",
                        color = AppColors.Secondary
                    )
                }
            }
        }
    }
}

// =======================================================
// APP PRINCIPAL (DASHBOARD, ETC)
// =======================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun App(userId: String, onLogout: () -> Unit) {
    var currentScreen: ScreenItem by remember { mutableStateOf(ScreenItem.Dashboard) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }

    // Lista de transações
    var transactions by remember { mutableStateOf(listOf<Transaction>()) }
    var isLoadingData by remember { mutableStateOf(true) }

    // Referência ao Firebase Database
    val database = Firebase.database
    val myRef = database.getReference("users").child(userId).child("transactions")

    // Carregamento Otimizado
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<Transaction>()
                for (child in snapshot.children) {
                    val transaction = child.getValue(Transaction::class.java)
                    if (transaction != null) {
                        items.add(transaction)
                    }
                }
                // OTIMIZAÇÃO: Inverte a lista aqui (uma única vez)
                transactions = items.reversed()
                isLoadingData = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoadingData = false
            }
        }
        myRef.addValueEventListener(listener)
        onDispose {
            myRef.removeEventListener(listener)
        }
    }

    val pagerState = rememberPagerState { mainScreens.size }

    LaunchedEffect(currentScreen) {
        val targetIndex = mainScreens.indexOf(currentScreen)
        if (targetIndex != -1) pagerState.animateScrollToPage(targetIndex)
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            currentScreen = mainScreens[pagerState.currentPage]
        }
    }

    // Modal de Adição
    if (showAddTransactionSheet) {
        ModalBottomSheet(onDismissRequest = { showAddTransactionSheet = false }) {
            AddTransactionScreen(
                onAddTransaction = { description, value, type, category ->
                    val key = myRef.push().key ?: return@AddTransactionScreen
                    // Data automática (Requer MinSdk 26)
                    val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

                    val newTransaction = Transaction(
                        id = key,
                        description = description,
                        value = value,
                        type = type,
                        date = currentDate,
                        category = category
                    )
                    myRef.child(key).setValue(newTransaction)
                    showAddTransactionSheet = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentScreen.topAppBarItem.title, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Primary),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sair", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AppColors.Surface, tonalElevation = 5.dp) {
                mainScreens.forEach { screen ->
                    val item = screen.bottomAppBarItem ?: return@forEach
                    NavigationBarItem(
                        selected = screen == currentScreen,
                        onClick = { currentScreen = screen },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.Primary,
                            selectedTextColor = AppColors.Primary,
                            unselectedIconColor = AppColors.Secondary.copy(alpha = 0.7f),
                            unselectedTextColor = AppColors.Secondary.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTransactionSheet = true },
                containerColor = AppColors.Primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }
    ) { innerPadding ->
        if (isLoadingData) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else {
            HorizontalPager(pagerState, Modifier.padding(innerPadding)) { page ->
                when (mainScreens[page]) {
                    ScreenItem.Dashboard -> DashboardScreen(transactions)
                    ScreenItem.Transactions -> TransactionsScreen(transactions)
                    ScreenItem.Settings -> SettingsScreen()
                }
            }
        }
    }
}

// =======================================================
// TELAS INTERNAS OTIMIZADAS
// =======================================================

@Composable
fun DashboardScreen(transactions: List<Transaction>) {
    // OTIMIZAÇÃO: remember evita recálculos desnecessários na UI
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == TransactionType.RECEITA }.sumOf { it.value }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == TransactionType.DESPESA }.sumOf { it.value }
    }
    val balance = remember(totalIncome, totalExpense) {
        totalIncome - totalExpense
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppColors.Background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Primary)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Balanço Atual", fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(
                        "R$ ${"%.2f".format(balance)}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryCard("Receitas", totalIncome, AppColors.Income, Modifier.weight(1f))
                SummaryCard("Despesas", totalExpense, AppColors.Expense, Modifier.weight(1f))
            }
        }

        item {
            Text(
                "Transações Recentes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        // A lista já está invertida (do mais novo pro mais antigo), pegamos apenas 5
        items(transactions.take(5)) { transaction ->
            TransactionItemCard(transaction)
        }
    }
}

@Composable
fun SummaryCard(title: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, color = AppColors.Secondary)
            Text(
                "R$ ${"%.2f".format(value)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TransactionsScreen(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhuma transação registrada.", color = AppColors.Secondary, fontSize = 18.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(AppColors.Background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "HISTÓRICO COMPLETO",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.Primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            // Usa lista já invertida no listener
            items(transactions, key = { it.id }) { transaction ->
                TransactionItemCard(transaction)
            }
        }
    }
}

@Composable
fun TransactionItemCard(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(transaction.description, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Text)
                Text("${transaction.category} - ${transaction.date}", color = AppColors.Secondary, fontSize = 14.sp)
            }
            Text(
                text = "${if (transaction.type == TransactionType.DESPESA) "-" else "+"} R$ ${"%.2f".format(transaction.value)}",
                color = if (transaction.type == TransactionType.DESPESA) AppColors.Expense else AppColors.Income,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onAddTransaction: (description: String, value: Double, type: TransactionType, category: String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DESPESA) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Adicionar Transação", style = MaterialTheme.typography.headlineSmall)

        SegmentedButtonRow(selectedType, onTypeChange = { selectedType = it })

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descrição") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Valor (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Categoria") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val valueDouble = amount.replace(",", ".").toDoubleOrNull()
                if (description.isNotBlank() && valueDouble != null && category.isNotBlank()) {
                    onAddTransaction(description, valueDouble, selectedType, category)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("SALVAR", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SegmentedButtonRow(
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp),
            onClick = { onTypeChange(TransactionType.DESPESA) },
            selected = selectedType == TransactionType.DESPESA,
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = AppColors.Expense.copy(alpha = 0.2f),
                activeContentColor = AppColors.Expense,
                inactiveContainerColor = Color.Transparent,
                activeBorderColor = AppColors.Expense
            ),
            icon = {}
        ) {
            Text("Despesa")
        }
        SegmentedButton(
            shape = RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp),
            onClick = { onTypeChange(TransactionType.RECEITA) },
            selected = selectedType == TransactionType.RECEITA,
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = AppColors.Income.copy(alpha = 0.2f),
                activeContentColor = AppColors.Income,
                inactiveContainerColor = Color.Transparent,
                activeBorderColor = AppColors.Income
            ),
            icon = {}
        ) {
            Text("Receita")
        }
    }
}

@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("Configurações", fontSize = 22.sp, color = AppColors.Text)
            Text("Você está logado com email e senha.", color = AppColors.Secondary, textAlign = TextAlign.Center)
        }
    }
}