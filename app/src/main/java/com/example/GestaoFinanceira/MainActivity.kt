package com.example.GestaoFinanceira // Mudei o nome do pacote

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GestaoFinanceira.ui.theme.AulaTelasTheme // Ajuste o import se o nome do tema for diferente

// =======================================================
// CONSTANTES E DATAS
// =======================================================

object AppColors {
    val Primary = Color(0xFF1976D2) // Azul Sólido
    val Secondary = Color(0xFF616161) // Cinza Escuro
    val Background = Color(0xFFF5F5F5)
    val Surface = Color.White
    val Text = Color(0xFF212121)
    val Income = Color(0xFF388E3C)   // Verde para receitas
    val Expense = Color(0xFFD32F2F)  // Vermelho para despesas
}

enum class TransactionType { RECEITA, DESPESA }

data class Transaction(
    val id: Int,
    val description: String,
    val value: Double,
    val type: TransactionType,
    val date: String, // Simplificado para String por enquanto
    val category: String
)

// =======================================================
// ESTRUTURA DE NAVEGAÇÃO
// =======================================================

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
// MAIN ACTIVITY E TEMA
// =======================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AulaTelasTheme { // Use o nome do seu tema
                MinimalTheme {
                    App()
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
// FUNÇÃO PRINCIPAL DO APP E NAVEGAÇÃO
// =======================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun App() {
    // Estado para a tela atual selecionada na BottomBar
    var currentScreen: ScreenItem by remember { mutableStateOf(ScreenItem.Dashboard) }

    // Estado para controlar a abertura da tela de adição
    var showAddTransactionSheet by remember { mutableStateOf(false) }

    // Lista de transações (dados de exemplo)
    var transactions by remember {
        mutableStateOf(listOf(
            Transaction(1, "Salário de Setembro", 4500.0, TransactionType.RECEITA, "05/10/2025", "Salário"),
            Transaction(2, "Aluguel", 1200.0, TransactionType.DESPESA, "06/10/2025", "Moradia"),
            Transaction(3, "Supermercado", 650.0, TransactionType.DESPESA, "07/10/2025", "Alimentação"),
            Transaction(4, "Freelance Website", 950.0, TransactionType.RECEITA, "08/10/2025", "Renda Extra"),
            Transaction(5, "Conta de Luz", 180.0, TransactionType.DESPESA, "09/10/2025", "Contas"),
        ))
    }

    val pagerState = rememberPagerState { mainScreens.size }

    // Função para adicionar uma nova transação
    val addTransaction: (Transaction) -> Unit = { transaction ->
        transactions = transactions + transaction
    }

    // Efeito para sincronizar a seleção da BottomBar com o Pager
    LaunchedEffect(currentScreen) {
        val targetIndex = mainScreens.indexOf(currentScreen)
        if (targetIndex != -1) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    // Efeito para sincronizar o Pager com a seleção da BottomBar
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            currentScreen = mainScreens[pagerState.currentPage]
        }
    }

    // Tela para adicionar transação (Modal)
    if (showAddTransactionSheet) {
        ModalBottomSheet(onDismissRequest = { showAddTransactionSheet = false }) {
            AddTransactionScreen(
                onAddTransaction = { description, value, type, category ->
                    val newId = (transactions.maxOfOrNull { it.id } ?: 0) + 1
                    val newTransaction = Transaction(newId, description, value, type, "10/10/2025", category) // Data fixa
                    addTransaction(newTransaction)
                    showAddTransactionSheet = false // Fecha o modal
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentScreen.topAppBarItem.title, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Primary)
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
                Icon(Icons.Default.Add, contentDescription = "Adicionar Transação")
            }
        }
    ) { innerPadding ->
        HorizontalPager(pagerState, Modifier.padding(innerPadding)) { page ->
            when (mainScreens[page]) {
                ScreenItem.Dashboard -> DashboardScreen(transactions)
                ScreenItem.Transactions -> TransactionsScreen(transactions)
                ScreenItem.Settings -> SettingsScreen()
            }
        }
    }
}

// =======================================================
// TELAS E COMPONENTES
// =======================================================

@Composable
fun DashboardScreen(transactions: List<Transaction>) {
    val totalIncome = transactions.filter { it.type == TransactionType.RECEITA }.sumOf { it.value }
    val totalExpense = transactions.filter { it.type == TransactionType.DESPESA }.sumOf { it.value }
    val balance = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppColors.Background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Card do Balanço Geral
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
            // Cards de Receita e Despesa
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

        // Lista das últimas 5 transações
        items(transactions.takeLast(5).reversed()) { transaction ->
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
            items(transactions.reversed(), key = { it.id }) { transaction ->
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

        // Seleção de Tipo (Receita/Despesa)
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
            Text("Tela de Configurações", fontSize = 22.sp, color = AppColors.Text)
            Text("Funcionalidades como tema, moeda, etc. apareceriam aqui.", color = AppColors.Secondary, textAlign = TextAlign.Center)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewApp() {
    MinimalTheme { App() }
}