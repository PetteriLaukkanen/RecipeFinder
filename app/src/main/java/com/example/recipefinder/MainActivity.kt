package com.example.recipefinder

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.rememberAsyncImagePainter
import com.example.recipes.ui.theme.RecipesTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.google.gson.Gson
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R) // Ensure the API level is R or higher for insets support
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Disable system window insets and set content layout to not fit system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Safely access the WindowInsetsController to handle system bars behavior
        window.decorView.post {
            // Get the WindowInsetsController to control the system UI visibility
            val controller = window.insetsController
            if (controller != null) {
                // Set behavior to allow transient bars to appear when swiped
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                // Hide system bars (e.g., status bar and navigation bar)
                controller.hide(android.view.WindowInsets.Type.systemBars())
            } else {
                // Log an error if WindowInsetsController is null
                Log.e("MainActivity", "WindowInsetsController is null.")
            }
        }

        // Set the content view for the activity with the custom theme and navigation
        setContent {
            RecipesTheme {
                AppNavigation()
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    // Remember the state of the pager, starting at the second page (index 1)
    val pagerState = rememberPagerState(1)
    // Remember the coroutine scope within the Composable function
    val coroutineScope = rememberCoroutineScope()

    // Create a Scaffold layout with a top bar containing icons
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {}, // No title for the top bar
                actions = {
                    // Show the top bar actions only when on the second page (index 1)
                    if (pagerState.currentPage == 1) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween, // Space icons on the left and right
                            modifier = Modifier.fillMaxWidth() // Ensure the row uses full width
                        ) {
                            // Left-side heart icon (for favorites)
                            IconButton(onClick = {
                                // Launch coroutine to animate scroll to the first page
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0) // Scroll to the first page
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = stringResource(R.string.favorite_icon)
                                )
                            }

                            // Right-side hamburger icon (menu)
                            IconButton(onClick = {
                                // Launch coroutine to animate scroll to the third page
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2) // Scroll to the third page
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.hamburger_icon)
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // HorizontalPager to display multiple screens
        HorizontalPager(
            count = 3, // Total 3 pages
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from Scaffold
        ) { page ->
            when (page) {
                0 -> FavouritesScreen() // First screen: Favourites
                1 -> HomeScreen() // Second screen: Home
                2 -> RecipeCategoriesScreen() // Third screen: Recipe Categories
            }
        }

        // Box to hold the pager indicator at the bottom center
        Box(
            modifier = Modifier
                .fillMaxSize() // Fill the entire screen
        ) {
            HorizontalPagerIndicator(
                pagerState = pagerState, // Bind the indicator to the pager state
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Position the indicator at the bottom center
                    .padding(8.dp), // Add padding around the indicator
                activeColor = MaterialTheme.colorScheme.primary // Use the primary color for the active indicator
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HomeScreen() {
    var showPopup by remember { mutableStateOf(false) } // State to control if the popup is shown
    var randomRecipe by remember { mutableStateOf<RecipeItem?>(null) } // State to hold the fetched random recipe
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for launching asynchronous tasks

    // Function to fetch a random recipe when the button is clicked
    fun fetchRandomRecipe() {
        coroutineScope.launch {
            randomRecipe = fetchRandomRecipeFromApi() // Fetch the recipe asynchronously
            showPopup = true // Show the popup once the recipe is fetched
        }
    }

    // Column layout for the HomeScreen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center, // Vertically center the content
        horizontalAlignment = Alignment.CenterHorizontally // Horizontally center the content
    ) {
        // Title text for the app
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Welcome text
        Text(
            text = stringResource(R.string.Welcome),
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button to fetch a random recipe
        Button(onClick = { fetchRandomRecipe() }) {
            Text(stringResource(R.string.find_something_new))
        }
    }

    // Show the popup with the random recipe if it has been fetched
    if (showPopup && randomRecipe != null) {
        RecipePopup(
            recipe = randomRecipe!!, // Unwrap the non-null random recipe
            onDismiss = { showPopup = false } // Close the popup when dismissed
        )
    }
}

// Fetch a random recipe from the API
suspend fun fetchRandomRecipeFromApi(): RecipeItem? {
    return try {
        val response = ApiService.getApi().getRandomRecipe() // Fetch data from the API
        response.meals.firstOrNull() // Return the first meal or null if empty
    } catch (e: Exception) {
        null // Return null in case of an error
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen() {
    val context = LocalContext.current
    val favourites = remember { mutableStateOf(getFavourites(context).toMutableList()) }
    var selectedRecipe by remember { mutableStateOf<RecipeItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.favourites)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Refresh the favourites list by reloading from storage
                favourites.value = getFavourites(context).toMutableList()
            }) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh_favourites))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(favourites.value) { recipe ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { selectedRecipe = recipe },
                    shape = MaterialTheme.shapes.medium
                ) {
                    // Add a remove button to each favourite recipe
                    RecipeItem(
                        meal = recipe,
                        onRemove = {
                            val updatedList = favourites.value.toMutableList().apply { remove(recipe) }
                            favourites.value = updatedList // Trigger recomposition
                            removeFromFavourites(context, recipe) // Remove from storage
                        }
                    )
                }
            }
        }
    }

    // Show the RecipePopup when a recipe is selected
    selectedRecipe?.let {
        RecipePopup(
            recipe = it,
            onDismiss = { selectedRecipe = null },
            isFavouritesScreen = true
        )
    }
}

@Composable
fun RecipePopup(
    recipe: RecipeItem,
    onDismiss: () -> Unit, // Function to dismiss the popup
    isFavouritesScreen: Boolean = false // Flag to determine if we're in the favorites screen
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var mealDetails by remember { mutableStateOf<RecipeDetails?>(null) } // State to hold the recipe details

    // Fetch recipe details asynchronously when the popup is shown
    LaunchedEffect(recipe.idMeal) {
        coroutineScope.launch {
            mealDetails = fetchRecipeDetails(recipe.idMeal) // Fetch detailed information of the recipe
        }
    }

    // Popup background with a clickable dismiss area
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)) // Semi-transparent background
            .clickable { onDismiss() }, // Dismiss the popup when clicked outside
        contentAlignment = Alignment.Center
    ) {
        // Card layout for the recipe details
        Card(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clickable(enabled = false) {}, // Disable clicking inside the card
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {

                // Close button at the top right
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(), // Fill the entire row width
                        horizontalArrangement = Arrangement.End // Align content to the right
                    ) {
                        Button(onClick = onDismiss) { // Button to close the popup
                            Text("X") // Text for the close button
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Recipe name
                item {
                    Text(
                        text = recipe.strMeal,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Recipe image
                item {
                    Image(
                        painter = rememberAsyncImagePainter(recipe.strMealThumb),
                        contentDescription = recipe.strMeal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 8.dp)
                    )
                }

                // Add to favorites button if not on the favorites screen
                if (!isFavouritesScreen) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            // Save recipe to favorites
                            saveToFavourites(context, recipe)
                            Toast.makeText(context,
                                context.getString(R.string.added_to_favourites), Toast.LENGTH_SHORT).show()
                        }) {
                            Text(stringResource(R.string.add_to_favourites))
                        }
                    }
                }

                // Display ingredients if available
                if (mealDetails != null) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ingredients),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    mealDetails!!.ingredients.zip(mealDetails!!.measures).forEachIndexed { index, (ingredient, measure) ->
                        if (ingredient.isNotEmpty()) {
                            item {
                                Text(
                                    text = "${index + 1}: $ingredient ($measure)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Recipe instructions
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.instructions),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    // Display parsed instructions
                    val instructions = mealDetails?.instructions ?: stringResource(R.string.no_instructions_available)
                    val instructionSteps = parseInstructions(instructions)

                    instructionSteps.forEachIndexed { index, step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Share button at the bottom
                item {
                    if (mealDetails != null) {
                        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            ShareButton(
                                recipeName = recipe.strMeal,
                                ingredients = mealDetails!!.ingredients,
                                measures = mealDetails!!.measures
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCategoriesScreen() {
    var categories by remember { mutableStateOf<List<CategoryResponse>?>(null) } // State to hold the list of categories
    var selectedCategory by remember { mutableStateOf<String?>(null) } // State to hold the selected category
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for launching background tasks

    // Fetch categories when the screen is first created
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            categories = fetchCategories() // Fetch the category data asynchronously
        }
    }

    // Scaffold to lay out the screen with padding and a LazyColumn for the categories list
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 16.dp,
                    bottom = 20.dp,
                    start = 5.dp,
                    end = 5.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Add space between list items
        ) {
            // If categories are available, display them
            if (categories != null) {
                items(categories!!) { category ->
                    RecipeCategoryItem(
                        category = category,
                        onCategorySelected = { selectedCategory = it } // Update selected category when clicked
                    )
                }
            } else {
                // Show a progress indicator while categories are being loaded
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // If a category is selected, display a popup with a list of recipes
        selectedCategory?.let {
            RecipePopupList(
                category = it,
                onDismiss = { selectedCategory = null } // Dismiss the popup when clicked outside
            )
        }
    }
}

// Fetch the list of categories from the API
suspend fun fetchCategories(): List<CategoryResponse> {
    return try {
        ApiService.getApi().getCategories().categories // Return the categories from the API
    } catch (e: Exception) {
        emptyList() // Return an empty list in case of an error
    }
}

@Composable
fun RecipeCategoryItem(category: CategoryResponse, onCategorySelected: (String) -> Unit) {
    // Display each category as a card with a click action
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCategorySelected(category.strCategory) }, // Trigger selection of category
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically // Align items vertically
        ) {
            // Category image
            Image(
                painter = rememberAsyncImagePainter(category.strCategoryThumb),
                contentDescription = category.strCategory,
                modifier = Modifier
                    .size(50.dp)
                    .padding(end = 8.dp)
            )
            Column {
                // Category name
                Text(
                    text = category.strCategory,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                // Category description (max 2 lines)
                Text(
                    text = category.strCategoryDescription,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RecipePopupList(
    category: String,
    onDismiss: () -> Unit // Function to dismiss the popup
) {
    var recipes by remember { mutableStateOf<List<RecipeItem>?>(null) } // State to hold the recipes for the selected category
    var expandedRecipeId by remember { mutableStateOf<String?>(null) } // State to track the currently expanded recipe
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for background tasks

    // Fetch recipes for the selected category when the popup opens
    LaunchedEffect(category) {
        coroutineScope.launch {
            recipes = fetchRecipesByCategory(category) // Fetch recipes asynchronously
        }
    }

    // Popup layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp) // Padding to keep popup inside the screen bounds
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)) // Semi-transparent background
            .clickable { onDismiss() }, // Dismiss the popup when clicked outside
        contentAlignment = Alignment.Center
    ) {
        // Card layout for the recipe list
        Card(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clickable(enabled = false) {}, // Disable clicking inside the card
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Display the category name at the top
                Text(
                    text = category,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Display recipes if they are loaded
                if (recipes != null) {
                    LazyColumn {
                        items(recipes!!) { meal ->
                            ExpandableRecipeItem(
                                meal = meal,
                                isExpanded = expandedRecipeId == meal.idMeal, // Check if the recipe is expanded
                                onClick = { id ->
                                    // Toggle the expanded recipe
                                    expandedRecipeId = if (expandedRecipeId == id) null else id
                                }
                            )
                        }
                    }
                } else {
                    // Show a progress indicator while recipes are being loaded
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


@Composable
fun ExpandableRecipeItem(
    meal: RecipeItem,      // The recipe item to display
    isExpanded: Boolean,   // Boolean to control if the recipe is expanded or collapsed
    onClick: (String) -> Unit, // Callback to handle recipe click (expansion)
) {
    var mealDetails by remember { mutableStateOf<RecipeDetails?>(null) } // State to hold the fetched recipe details
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for launching background tasks
    val context = LocalContext.current // Context to interact with the local environment (e.g., to show Toasts)

    // Card to display the meal item
    Card(
        modifier = Modifier
            .fillMaxWidth() // Fill available width
            .padding(bottom = 8.dp) // Add space below the card
            .clickable {
                // Handle click event to toggle expansion and fetch details if necessary
                onClick(meal.idMeal)
                if (!isExpanded) {
                    coroutineScope.launch {
                        mealDetails =
                            fetchRecipeDetails(meal.idMeal) // Fetch recipe details when expanding
                    }
                }
            },
        shape = MaterialTheme.shapes.medium, // Medium rounded corners for the card
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Add slight shadow for elevation
    ) {
        Column(
            modifier = Modifier.padding(8.dp) // Padding around the card content
        ) {
            // Row for meal image and title
            Row(
                verticalAlignment = Alignment.CenterVertically // Vertically align items in the center
            ) {
                // Display meal image with a fixed size
                Image(
                    painter = rememberAsyncImagePainter(meal.strMealThumb),
                    contentDescription = meal.strMeal,
                    modifier = Modifier
                        .size(60.dp) // Set fixed size for the image
                        .padding(end = 8.dp) // Add space after the image
                )
                // Display meal name
                Text(
                    text = meal.strMeal,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, // Make the title bold
                    modifier = Modifier.padding(bottom = 4.dp) // Add padding below the text
                )
            }

            // If the item is expanded, show additional details
            if (isExpanded) {
                // Spacer for separation between UI elements
                Spacer(modifier = Modifier.height(8.dp))

                // Button to add the recipe to favorites
                Button(
                    onClick = {
                        saveToFavourites(context, meal) // Save recipe to favorites
                        Toast.makeText(context, R.string.added_to_favourites, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth() // Button takes full width of its container
                ) {
                    Text (stringResource(R.string.add_to_favourites)) // Button text
                }

                // Display recipe details when expanded
                if (mealDetails != null) {
                    // Parse and display instructions
                    val instructionsSteps = parseInstructions(mealDetails!!.instructions)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display category of the recipe
                    Text(
                        text = stringResource(R.string.ingredients),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Loop through ingredients and measures and display them
                    mealDetails!!.ingredients.zip(mealDetails!!.measures).forEachIndexed { index, (ingredient, measure) ->
                        if (ingredient.isNotEmpty()) {
                            Text(
                                text = "${index + 1}: $ingredient ($measure)", // Display ingredient with its measure
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display instructions section header
                    Text(
                        text = stringResource(R.string.instructions),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Display each step of the instructions
                    instructionsSteps.forEachIndexed { index, step ->
                        Text(
                            text = step, // Display individual instruction step
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp)) // Space between each instruction step
                    }

                    // Share button to share the recipe
                    Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        ShareButton(
                            recipeName = meal.strMeal, // Recipe name to share
                            ingredients = mealDetails!!.ingredients, // Ingredients list to share
                            measures = mealDetails!!.measures // Measures list to share
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ShareButton(recipeName: String, ingredients: List<String>, measures: List<String>) {
    // Get the current context for starting an activity
    val context = LocalContext.current

    // Create a string combining the recipe name, ingredients, and their respective measures
    val shareText = remember {
        // Pair ingredients with their measures and filter out empty entries
        val ingredientsText = ingredients.zip(measures)
            .filter { (ingredient, measure) -> ingredient.isNotEmpty() && measure.isNotEmpty() }
            // Combine ingredient and measure pairs into a formatted string
            .joinToString("\n") { (ingredient, measure) -> "$ingredient: $measure" }

        // Create the final shareable text: recipe name followed by ingredients and their measures
        "$recipeName $ingredients\n$ingredientsText"
    }

    // Button to trigger the sharing action
    Button(
        onClick = {
            // Create an intent to share the text as plain text
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            // Start the share activity with a chooser to let the user pick an app to share the text
            context.startActivity(Intent.createChooser(sendIntent,
                context.getString(R.string.share_ingredients)))
        }
    ) {
        // Display the button text
        Text(context.getString(R.string.share_ingredients))
    }
}

// Function to fetch recipe details asynchronously by meal ID
suspend fun fetchRecipeDetails(idMeal: String): RecipeDetails? {
    return try {
        // Make a network call to fetch recipe details from the API
        val response = ApiService.getApi().getRecipeDetails(idMeal)

        // Map the response to RecipeDetails and return the first meal, or null if no meal is found
        response.meals.firstOrNull()?.toRecipeDetails()
    } catch (e: Exception) {
        // Return null if there's an error during the network call
        null
    }
}

// Data class to hold recipe details (not included here, assumed to be defined elsewhere)

// Extension function to map RecipeItem to RecipeDetails
fun RecipeItem.toRecipeDetails(): RecipeDetails {
    // Create a list of ingredients by including non-null ingredient fields
    val ingredients = listOfNotNull(
        strIngredient1, strIngredient2, strIngredient3, strIngredient4, strIngredient5,
        strIngredient6, strIngredient7, strIngredient8, strIngredient9, strIngredient10,
        strIngredient11, strIngredient12, strIngredient13, strIngredient14, strIngredient15,
        strIngredient16, strIngredient17, strIngredient18, strIngredient19, strIngredient20
    )

    // Create a list of measures by including non-null measure fields
    val measures = listOfNotNull(
        strMeasure1, strMeasure2, strMeasure3, strMeasure4, strMeasure5,
        strMeasure6, strMeasure7, strMeasure8, strMeasure9, strMeasure10,
        strMeasure11, strMeasure12, strMeasure13, strMeasure14, strMeasure15,
        strMeasure16, strMeasure17, strMeasure18, strMeasure19, strMeasure20
    )

    // Return a RecipeDetails object with instructions, category, area, ingredients, and measures
    return RecipeDetails(
        instructions = this.strInstructions,
        category = this.strCategory,
        area = this.strArea,
        ingredients = ingredients,
        measures = measures
    )
}

@Composable
fun RecipeItem(meal: RecipeItem, onRemove: (() -> Unit)? = null) {

        // Row layout to arrange the image and text horizontally
        Row(
            modifier = Modifier
                .padding(8.dp), // Adds padding inside the row
            verticalAlignment = Alignment.CenterVertically // Aligns items vertically at the center
        ) {
            // Meal image displayed with a specified size and padding
            Image(
                painter = rememberAsyncImagePainter(meal.strMealThumb), // Loads the image asynchronously
                contentDescription = meal.strMeal, // Description of the meal (for accessibility)
                modifier = Modifier
                    .size(60.dp) // Fixed size for the image
                    .padding(end = 8.dp) // Adds padding on the right of the image
            )

            // Column to display meal details like name
            Column(
                modifier = Modifier.weight(1f) // Makes the column take up available space
            ) {
                // Text displaying the meal name with bold styling
                Text(
                    text = meal.strMeal, // Meal name
                    style = MaterialTheme.typography.bodyMedium, // Text style from the theme
                    fontWeight = FontWeight.Bold, // Bold font weight
                    modifier = Modifier.padding(bottom = 4.dp) // Adds padding below the text
                )
            }

            // "X" button for removing the meal from favourites, if onRemove is provided
            onRemove?.let {
                IconButton(onClick = it) { // Executes the onRemove function when clicked
                    Icon(
                        imageVector = Icons.Default.Close, // "Close" icon (X)
                        contentDescription = stringResource(R.string.remove_favourite) // Description for accessibility
                    )
                }
            }
        }
    }

// Suspend function to fetch recipes by category asynchronously
suspend fun fetchRecipesByCategory(category: String): List<RecipeItem>? {
    return try {
        // Make an API request to fetch meals by the specified category
        val response = ApiService.getApi().getMealsByCategory(category)

        // Return the list of meals (RecipeItems) if the request is successful
        response.meals
    } catch (e: Exception) {
        // Return null in case of an exception (e.g., network error)
        null
    }
}

// Function to parse recipe instructions into a list of steps
fun parseInstructions(instructions: String): List<String> {
    // Split the instructions string into separate lines based on newline characters
    return instructions.split(Regex("\\r?\\n"))
        .filter { it.isNotBlank() }  // Remove blank lines from the instructions
        .map { it.trim() }  // Trim any leading or trailing spaces from each line
}
// Function to save a recipe to favourites using SharedPreferences
fun saveToFavourites(context: Context, recipe: RecipeItem) {
    // Get the SharedPreferences instance with a name "Favourites" and private mode
    val sharedPreferences = context.getSharedPreferences(context.getString(R.string.favourites), Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()

    // Convert the recipe object to a JSON string using Gson
    val recipeJson = Gson().toJson(recipe)

    // Save the JSON string in SharedPreferences with the recipe's idMeal as the key
    editor.putString(recipe.idMeal, recipeJson)

    // Apply the changes to SharedPreferences asynchronously
    editor.apply()
}

// Function to retrieve all favourite recipes from SharedPreferences
fun getFavourites(context: Context): List<RecipeItem> {
    // Get the SharedPreferences instance for the "Favourites" data
    val sharedPreferences = context.getSharedPreferences(context.getString(R.string.favourites), Context.MODE_PRIVATE)

    // Retrieve all stored recipes as a map of key-value pairs
    val allRecipes = sharedPreferences.all

    // List to hold the favourite recipes
    val favourites = mutableListOf<RecipeItem>()
    val gson = Gson()

    // Iterate through all stored entries in SharedPreferences
    allRecipes.forEach { (_, value) ->
        // The value is a JSON string, so we deserialize it into a RecipeItem object
        val recipeJson = value as String
        val recipe = gson.fromJson(recipeJson, RecipeItem::class.java)

        // Add the deserialized recipe to the favourites list
        favourites.add(recipe)
    }

    // Return the list of favourite recipes
    return favourites
}

// Function to remove a recipe from shared preferences
fun removeFromFavourites(context: Context, recipe: RecipeItem) {
    val sharedPreferences = context.getSharedPreferences(context.getString(R.string.favourites), Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.remove(recipe.idMeal) // Remove the recipe using its ID
    editor.apply()
}


