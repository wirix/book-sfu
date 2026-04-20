package com.example.chitfin

import androidx.compose.material.icons.filled.MenuBook
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chitfin.data.User
import com.example.chitfin.data.UserPreferences
import com.example.chitfin.ui.theme.ChitFinTheme
import kotlinx.coroutines.launch
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import androidx.compose.material.icons.filled.PictureAsPdf
import com.example.chitfin.data.PdfStorage
import android.content.Context
import com.example.chitfin.data.copyPdfToInternalStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.lazy.items
import java.io.FileOutputStream
import androidx.compose.foundation.clickable
import com.example.chitfin.ui.PdfViewerScreen
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.style.TextOverflow

// Список экранов для bottom bar
sealed class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
) {
    data object Library : BottomNavItem("library", Icons.Default.Home, "Домой")
    data object MyBooks : BottomNavItem("mybooks", Icons.Default.MailOutline, "Книги")
}

class MainActivity : ComponentActivity() {

    private lateinit var userPrefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPrefs = UserPreferences(this)

        enableEdgeToEdge()

        setContent {
            ChitFinTheme {
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()

                var currentUser by remember { mutableStateOf<User?>(null) }

                LaunchedEffect(Unit) {
                    userPrefs.userFlow.collect { user ->
                        currentUser = user
                    }
                }

                if (currentUser == null) {
                    // Экран авторизации / регистрации
                    NavHost(navController = navController, startDestination = "start") {
                        composable("start") {
                            StartScreen(
                                onStartReading = { navController.navigate("register") },
                                onLogin = { navController.navigate("login") }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = { newUser ->
                                    coroutineScope.launch {
                                        userPrefs.saveUser(newUser)
                                    }
                                }
                            )
                        }
                        composable("login") {
                            LoginScreenPlaceholder(navController)
                        }
                    }
                } else {
                    // Главный экран с Bottom Bar и фоном
                    MainAppScreen(navController)
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Library,
        BottomNavItem.MyBooks
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White
            ) {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFBB86FC),
                            unselectedIconColor = Color.White,
                            selectedTextColor = Color(0xFFBB86FC),
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Тот же фон на всех экранах после входа
            Image(
                painter = painterResource(id = R.drawable.screenshot1),
                contentDescription = "Фон",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)) // чуть темнее, чтобы текст лучше читался
            )

            // Контент в зависимости от вкладки
            // Контент в зависимости от вкладки
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Library.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavItem.Library.route) {
                    LibraryScreen()
                }
                composable(BottomNavItem.MyBooks.route) {
                    MyBooksScreen(navController = navController)  // ← теперь с параметром
                }

                // ← вот сюда вставляем новый экран
                composable("pdf_viewer/{fileName}") { backStackEntry ->
                    val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
                    PdfViewerScreen(
                        fileName = fileName,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryScreen() {
    // Пока заглушка для главной страницы
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Главная / Популярные книги",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(40.dp))
        Text("Здесь будет список популярных книг", color = Color.LightGray)
    }
}

@Composable
fun MyBooksScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pdfStorage = remember { PdfStorage(context) }
    val pdfFiles by pdfStorage.pdfFilesFlow.collectAsState(initial = emptySet<String>())

    val pdfList = pdfFiles.toList()

    // Состояния для диалога переименования
    var showRenameDialog by remember { mutableStateOf(false) }
    var currentFileName by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }

    // Функция удаления книги
    fun deleteBook(fileName: String) {
        coroutineScope.launch {
            val pdfDir = File(context.filesDir, "pdfs")
            val file = File(pdfDir, fileName)
            if (file.exists()) {
                file.delete()
            }
            pdfStorage.deletePdfFile(fileName)   // удаляем из DataStore
        }
    }

    // Функция переименования
    fun renameBook(oldName: String, newName: String) {
        if (newName.isBlank() || newName == oldName) return

        coroutineScope.launch {
            val success = pdfStorage.renamePdfFile(oldName, newName)
            if (success) {
                // Можно добавить Toast или Snackbar "Название изменено"
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            coroutineScope.launch {
                val savedName = copyPdfToInternalStorage(context, it)
                if (savedName != null) {
                    pdfStorage.addPdfFile(savedName)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Мои книги", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)

        if (pdfList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("У вас нет книг", fontSize = 24.sp, color = Color.LightGray)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(pdfList) { fileName ->
                    PdfItemRow(
                        fileName = fileName,
                        context = context,
                        onClick = {
                            navController.navigate("pdf_viewer/$fileName")
                        },
                        onRename = { oldName ->
                            currentFileName = oldName
                            newFileName = oldName
                            showRenameDialog = true
                        },
                        onDelete = {
                            deleteBook(fileName)
                        }
                    )
                }
            }
        }

        Button(
            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 16.dp)
        ) {
            Text("Загрузить PDF")
        }
    }

    // Диалог переименования
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Переименовать книгу") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("Новое название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        renameBook(currentFileName, newFileName)
                        showRenameDialog = false
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun PdfItemRow(
    fileName: String,
    context: Context,
    onClick: () -> Unit,        // открытие книги
    onRename: (String) -> Unit, // переименование
    onDelete: () -> Unit        // удаление
) {
    val pageCount = remember(fileName) { getPdfPageCount(context, fileName) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },           // ← Главный клик по всей карточке
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PdfPlaceholderIcon(Modifier.size(64.dp))

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fileName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (pageCount > 0) "$pageCount стр." else "страницы неизвестны",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }

            // Кнопка редактирования
            IconButton(onClick = { onRename(fileName) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Переименовать",
                    tint = Color.White.copy(alpha = 0.85f)
                )
            }

            // Кнопка удаления
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = Color.Red.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun PdfPlaceholderIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PictureAsPdf,  // или любая другая иконка
            contentDescription = "PDF",
            tint = Color.Red.copy(alpha = 0.7f),
            modifier = Modifier.size(40.dp)
        )
    }
}

private fun getPdfPageCount(context: Context, fileName: String): Int {
    val pdfDir = File(context.filesDir, "pdfs")
    val file = File(pdfDir, fileName)
    if (!file.exists()) return 0

    return try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                renderer.pageCount
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}

@Composable
fun StartScreen(
    onStartReading: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.screenshot1), // ← твой фон
            contentDescription = "Фон",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Читалка",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(80.dp))

            Button(
                onClick = onStartReading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Начать читать", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Войти", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Регистрация", fontSize = 32.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Подтвердите пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                when {
                    name.isBlank() -> error = "Введите имя"
                    email.isBlank() -> error = "Введите email"
                    password.isBlank() -> error = "Введите пароль"
                    password != confirmPassword -> error = "Пароли не совпадают"
                    else -> {
                        error = null
                        onRegisterSuccess(User(name.trim(), email.trim(), password))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Зарегистрироваться")
        }
    }
}

@Composable
fun LoginScreenPlaceholder(navController: androidx.navigation.NavController) {
    // Пока заглушка — потом сделаем полноценный экран входа
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Экран входа (пока заглушка)", fontSize = 24.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Назад")
        }
    }
}

// Автоматическое копирование встроенных PDF из assets при первом запуске
private suspend fun copyAssetsPdfsToInternal(context: Context, pdfStorage: PdfStorage) {
    val assetManager = context.assets
    val pdfDir = File(context.filesDir, "pdfs")
    if (!pdfDir.exists()) pdfDir.mkdirs()

    try {
        val files = assetManager.list("pdf") ?: emptyArray()
        files.forEach { fileName ->
            if (fileName.endsWith(".pdf", ignoreCase = true)) {
                val targetFile = File(pdfDir, fileName)

                // Копируем только если ещё нет
                if (!targetFile.exists()) {
                    assetManager.open("pdf/$fileName").use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    pdfStorage.addPdfFile(fileName)
                    println("Скопирован встроенный PDF: $fileName")
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}