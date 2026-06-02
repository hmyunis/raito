package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.util.CompanionRegistry
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaitoViewModel
import com.example.data.database.CustomAvatarEntity

data class ShopItem(
  val id: String,
  val name: String,
  val description: String,
  val imageUrl: String,
  val cost: Int,
  val category: String // Chibis, Outfits, Animals
)

@Composable
fun ShopView(
  viewModel: RaitoViewModel,
  modifier: Modifier = Modifier
) {
  val stats by viewModel.stats.collectAsState()
  val customAvatars by viewModel.customAvatars.collectAsState()

  var selectedCategory by remember { mutableStateOf("Chibis") }
  var showCreatorDialog by remember { mutableStateOf(false) }

  val craftCost = (1000 * viewModel.getShopPriceMultiplier()).toInt()

  val currentUnlocked = remember(stats.unlockedCompanions) {
    stats.unlockedCompanions.split(",").toSet()
  }

  val shopItems = remember {
    listOf(
      ShopItem(
        id = "Cyber",
        name = "Cyber Assistant",
        description = "Standard digital helper to navigate focused tasks.",
        imageUrl = CompanionRegistry.CYBER.neutral,
        cost = 0,
        category = "Chibis"
      ),
      ShopItem(
        id = "Knight",
        name = "Tiny Knight",
        description = "A brave warrior ready to defend your focus sessions.",
        imageUrl = CompanionRegistry.KNIGHT.neutral,
        cost = 500,
        category = "Chibis"
      ),
      ShopItem(
        id = "Scholar",
        name = "Studio Artist",
        description = "Pixel chibi with creative tools for artistic discipline.",
        imageUrl = CompanionRegistry.SCHOLAR.neutral,
        cost = 1500,
        category = "Chibis"
      ),
      ShopItem(
        id = "Ranger",
        name = "Shadow Ranger",
        description = "A stealthy rogue adept at hunting down distractions.",
        imageUrl = CompanionRegistry.RANGER.neutral,
        cost = 4000,
        category = "Chibis"
      ),
      ShopItem(
        id = "Dragon",
        name = "Dragon Keeper",
        description = "Master of mythical beasts. The ultimate focus companion.",
        imageUrl = CompanionRegistry.DRAGON.neutral,
        cost = 10000,
        category = "Chibis"
      )
    )
  }

  // Prepend or add custom avatars/designs
  val customShopItems = remember(customAvatars) {
    customAvatars.map { custom ->
      val displayName = custom.name.removePrefix("[Chibi] ")
      ShopItem(
        id = "custom_${custom.id}",
        name = displayName,
        description = "Custom crafted chibi assistant.",
        imageUrl = custom.neutralPath ?: CompanionRegistry.CYBER.neutral,
        cost = 0, // already unlocked since user crafted it
        category = "Chibis"
      )
    }
  }

  val filteredItems = remember(shopItems, customShopItems) {
    shopItems + customShopItems
  }

  var avatarToDelete by remember { mutableStateOf<ShopItem?>(null) }
  var previewItem by remember { mutableStateOf<ShopItem?>(null) }

  if (avatarToDelete != null) {
    DeleteConfirmationDialog(
      title = "DELETE CUSTOM AVATAR?",
      message = "Are you sure you want to permanently delete \"${avatarToDelete!!.name.uppercase()}\"? This action cannot be undone.",
      onConfirm = {
        val customId = avatarToDelete!!.id.removePrefix("custom_").toIntOrNull()
        val targetObj = customAvatars.find { it.id == customId }
        if (targetObj != null) {
          viewModel.deleteCustomAvatar(targetObj)
        }
        avatarToDelete = null
      },
      onDismiss = { avatarToDelete = null }
    )
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize()
  ) {
    val isLandscape = maxWidth > maxHeight || maxWidth > 600.dp
    val gridColumns = if (isLandscape) GridCells.Fixed(2) else GridCells.Fixed(1)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // Header Point summary box
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .mangaShadow(offset = 3.dp)
          .background(BgPaperLight)
          .mangaBorder()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "STUDIO SHOP",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
            color = InkBlack
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "COST RATE: ${viewModel.getShopPriceMultiplier()}x (${stats.difficulty.uppercase()})",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
          )
        }

        // Point balance indicator
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Point balance box
          Box(
            modifier = Modifier
              .background(Color.White)
              .mangaBorder()
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Points indicator",
                tint = AnimeYellow,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${stats.points} PTS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = InkBlack
              )
            }
          }

        }
      }

      if (!stats.isWelcomingGiftClaimed) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .mangaShadow(offset = 3.dp)
            .background(InkBlack)
            .mangaBorder(color = AnimeTeal, width = 2.dp)
            .padding(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
               Text(
                 text = "🎁 WELCOMING GIFT (+500 PTS)",
                 style = MaterialTheme.typography.titleSmall,
                 fontWeight = FontWeight.Black,
                 color = Color.White
               )
               Spacer(modifier = Modifier.height(2.dp))
               Text(
                 text = "Claim your +500 PTS welcoming bonus to jump-start your studio shop purchases!",
                 style = MaterialTheme.typography.bodySmall,
                 color = Color.LightGray,
                 lineHeight = 14.sp
               )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
              onClick = { viewModel.claimWelcomingGift() },
              shape = RectangleShape,
              colors = ButtonDefaults.buttonColors(containerColor = AnimeTeal, contentColor = Color.Black),
              modifier = Modifier.mangaBorder(width = 1.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Text("CLAIM", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
            }
          }
        }
      }

      // Dedicated Inside-Tab Crafting Banners based on tab selection
      val canAffordCraft = stats.points >= craftCost

      CraftBanner(
        category = "Chibis",
        cost = craftCost,
        canAfford = canAffordCraft,
        onClick = { showCreatorDialog = true }
      )

      // Shop item listing grid
      LazyVerticalGrid(
        columns = gridColumns,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
      ) {
        items(filteredItems) { item ->
          val isCustom = item.id.startsWith("custom_")
          val isUnlocked = isCustom || currentUnlocked.contains(item.id) || item.cost == 0

          val costMultiplier = viewModel.getShopPriceMultiplier()
          val calculatedCost = (item.cost * costMultiplier).toInt()
          val isEquipped = stats.activeCompanionId == item.id
          val isAffordable = stats.points >= calculatedCost

          ShopItemCard(
            item = item,
            calculatedCost = calculatedCost,
            isUnlocked = isUnlocked,
            isEquipped = isEquipped,
            isAffordable = isAffordable,
            onPurchaseClick = { viewModel.purchaseCompanion(item.id, calculatedCost) },
            onEquipClick = { viewModel.equipCompanion(item.id) },
            onPreviewClick = { previewItem = item },
            onDeleteClick = if (isCustom) {
              {
                avatarToDelete = item
              }
            } else null
          )
        }
      }
    }
  }

  // Adaptive Creator Builder Form Dialog
  if (showCreatorDialog) {
    var avatarName by remember { mutableStateOf("") }
    var neutralUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var happyUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var focusUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var sadUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var completedUri by remember { mutableStateOf<android.net.Uri?>(null) }

    var pendingExpression by remember { mutableStateOf<String?>(null) }

    val pickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
      contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
      if (uri != null) {
        when (pendingExpression) {
          "neutral" -> neutralUri = uri
          "happy" -> happyUri = uri
          "focus" -> focusUri = uri
          "sad" -> sadUri = uri
          "completed" -> completedUri = uri
        }
      }
    }

    androidx.compose.ui.window.Dialog(
      onDismissRequest = { showCreatorDialog = false },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(0.4f))
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .mangaShadow(offset = 6.dp)
            .background(BgPaperLight)
            .mangaBorder(width = 3.dp)
            .padding(16.dp)
        ) {
          CustomAvatarForm(
            avatarName = avatarName,
            onNameChange = { avatarName = it },
            neutralUri = neutralUri,
            happyUri = happyUri,
            focusUri = focusUri,
            sadUri = sadUri,
            completedUri = completedUri,
            onSelectImageClick = {
              pendingExpression = it
              pickerLauncher.launch("image/*")
            },
            onSave = {
              if (avatarName.trim().isNotEmpty() && neutralUri != null) {
                viewModel.createCustomAvatar(
                  name = "[Chibi] " + avatarName,
                  neutralUri = neutralUri,
                  happyUri = happyUri,
                  focusUri = focusUri,
                  sadUri = sadUri,
                  completedUri = completedUri,
                  cost = craftCost
                )
                showCreatorDialog = false
              }
            },
            onDiscard = { showCreatorDialog = false },
            saveButtonText = "SUBMIT & CRAFT (-$craftCost PTS)"
          )
        }
      }
    }
  }

  if (previewItem != null) {
    // Chibi preview dialog
    val targetObj = customAvatars.find { it.id.toString() == previewItem!!.id.removePrefix("custom_") }

    androidx.compose.ui.window.Dialog(
      onDismissRequest = { previewItem = null },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(0.5f))
          .clickable { previewItem = null }
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .mangaShadow(offset = 6.dp)
            .background(BgPaperLight)
            .mangaBorder()
            .clickable(enabled = false, onClick = {})
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = "CHIBI PREVIEWS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = InkBlack
          )
          
          val uris = if (targetObj != null) {
            listOf(
              "NEUTRAL" to targetObj.neutralPath,
              "HAPPY" to targetObj.happyPath,
              "FOCUS" to targetObj.focusPath,
              "SAD" to targetObj.sadPath,
              "COMPLETED" to targetObj.completedPath
            )
          } else {
            val expressions = CompanionRegistry.getExpressions(previewItem!!.id)
            listOf(
              "NEUTRAL" to expressions.neutral,
              "HAPPY" to expressions.happy,
              "FOCUS" to expressions.focus,
              "SAD" to expressions.sad,
              "COMPLETED" to expressions.completed
            )
          }

          LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(uris) { (label, uri) ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                  .mangaBorder()
                  .background(Color.White)
                  .padding(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .mangaBorder(1.dp)
                    .background(BgPaperLight),
                  contentAlignment = Alignment.Center
                ) {
                  if (uri != null) {
                    AsyncImage(model = uri, contentDescription = label, modifier = Modifier.fillMaxSize(0.82f))
                  } else {
                    Icon(imageVector = Icons.Default.Face, contentDescription = label, tint = InkGrayDark, modifier = Modifier.size(34.dp))
                  }
                }
                Text(
                  text = label,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = InkBlack,
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          Button(
            onClick = { previewItem = null },
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = InkBlack, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().mangaBorder()
          ) {
            Text("CLOSE PREVIEW", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

data class CraftBannerColors(
  val cardColor: Color,
  val textColor: Color,
  val title: String,
  val sub: String
)

@Composable
fun CraftBanner(
  category: String,
  cost: Int,
  canAfford: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bannerColors = when (category) {
    "Outfits" -> CraftBannerColors(AnimePurple, Color.White, "DESIGN CUSTOM GLOW OUTFIT", "Infuse neon border particles on focus areas (Costs $cost PTS)")
    "Animals" -> CraftBannerColors(AnimeTeal, Color.Black, "SUMMON MYSTICAL PT GUARDIAN", "Summon elemental pet entities to assist your logs (Costs $cost PTS)")
    else -> CraftBannerColors(AnimeYellow, Color.Black, "CRAFT CUSTOM CHIBI", "Submit custom PNG graphics as concierge sprites (Costs $cost PTS)")
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .mangaShadow(offset = 3.dp)
      .background(if (canAfford) bannerColors.cardColor else InkGrayLight)
      .mangaBorder()
      .clickable(enabled = canAfford) { onClick() }
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = bannerColors.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Black,
          color = bannerColors.textColor
        )
        Text(
          text = bannerColors.sub.uppercase(),
          style = MaterialTheme.typography.labelSmall,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = bannerColors.textColor.copy(alpha = 0.82f)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .background(if (canAfford) Color.White else InkGrayWash)
          .mangaBorder(width = 1.dp)
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(
            imageVector = if (canAfford) Icons.Default.AddCircle else Icons.Default.Lock,
            contentDescription = "Craft button action icon",
            tint = if (canAfford) InkBlack else InkGrayDark,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "CRAFT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = if (canAfford) InkBlack else InkGrayDark,
            fontSize = 9.sp
          )
        }
      }
    }
  }
}

@Composable
fun ShopItemCard(
  item: ShopItem,
  calculatedCost: Int,
  isUnlocked: Boolean,
  isEquipped: Boolean,
  isAffordable: Boolean,
  onPurchaseClick: () -> Unit,
  onEquipClick: () -> Unit,
  onPreviewClick: () -> Unit,
  onDeleteClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val cardTextColor = if (isUnlocked) InkBlack else MaterialTheme.colorScheme.onSurface
  val cardSubtextColor = if (isUnlocked) InkGrayDark else MaterialTheme.colorScheme.onSurfaceVariant

  Box(
    modifier = modifier
      .fillMaxWidth()
      .mangaShadow(offset = 3.dp)
      .background(if (isUnlocked) BgPaperLight else MaterialTheme.colorScheme.surface)
      .mangaBorder()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
    ) {
      // Chibi Drawing Panel with light Screentones
      Box(
        modifier = Modifier
          .size(120.dp)
          .mangaBorder()
          .background(Color.White)
          .clickable { onPreviewClick() }
          .screentonePattern(dotColor = InkBlack.copy(0.06f)),
        contentAlignment = Alignment.Center
      ) {
        AsyncImage(
          model = item.imageUrl,
          contentDescription = item.name,
          modifier = Modifier.fillMaxSize(0.75f)
        )

        if (isUnlocked && item.id != "Cyber" && onDeleteClick == null) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .background(AnimeGreen)
              .mangaBorder(1.dp)
              .padding(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Text(
              text = "UNLOCKED",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 7.sp
            )
          }
        }

        // Custom item tag indicator
        if (onDeleteClick != null) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .background(AnimeYellow)
              .mangaBorder(1.dp)
              .padding(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Text(
              text = "CUSTOM CRAFT",
              style = MaterialTheme.typography.labelSmall,
              color = Color.Black,
              fontWeight = FontWeight.Bold,
              fontSize = 7.sp
            )
          }
        }
      }

      // Details Block
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = item.name.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = cardTextColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
          )

          // Delete/Trash option for custom items
          if (onDeleteClick != null) {
            IconButton(
              onClick = { onDeleteClick() },
              modifier = Modifier.size(26.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete custom chibi",
                tint = AnimeRed,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = item.description,
          style = MaterialTheme.typography.bodyMedium,
          color = cardSubtextColor,
          maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        // CTA buttons
        if (isUnlocked) {
          Button(
            onClick = { onEquipClick() },
            shape = RectangleShape,
            modifier = Modifier
              .fillMaxWidth()
              .height(36.dp)
              .mangaBorder(),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isEquipped) AnimeTeal else Color.White,
              contentColor = if (isEquipped) Color.White else InkBlack
            )
          ) {
            Text(
              text = if (isEquipped) "EQUIPPED" else "EQUIP",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = if (isEquipped) Color.Black else InkBlack
            )
          }
        } else {
          Button(
            onClick = { onPurchaseClick() },
            enabled = isAffordable,
            shape = RectangleShape,
            modifier = Modifier
              .fillMaxWidth()
              .height(36.dp)
              .mangaBorder(),
            colors = ButtonDefaults.buttonColors(
              containerColor = InkBlack,
              contentColor = Color.White,
              disabledContainerColor = InkGrayLight,
              disabledContentColor = InkGrayDark
            )
          ) {
            Text(
              text = "$calculatedCost PTS | BUY",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
