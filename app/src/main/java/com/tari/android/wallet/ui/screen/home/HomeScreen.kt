package com.tari.android.wallet.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewPrimarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.home.HomeModel.BottomMenuOption
import com.tari.android.wallet.ui.screen.home.overview.HomeOverviewFragment
import com.tari.android.wallet.ui.screen.profile.login.ProfileLoginFragment
import com.tari.android.wallet.ui.screen.profile.profile.ProfileFragment
import com.tari.android.wallet.ui.screen.settings.allSettings.AllSettingsFragment
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.ui.screen.store.StoreFragment

private const val FRAGMENT_CONTAINER_ID = 1

@Composable
fun HomeScreen(
    uiState: HomeModel.UiState,
    fragmentManager: FragmentManager,
    onMenuItemClicked: (BottomMenuOption) -> Unit,
) {
    Scaffold(
        backgroundColor = TariDesignSystem.colors.backgroundSecondary,
        modifier = Modifier,
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .statusBarsPadding()
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
                    .navigationBarsPadding(),
            ) {
                when (uiState.selectedMenuItem) {
                    BottomMenuOption.Home -> FragmentContainer(
                        modifier = Modifier.fillMaxSize(),
                        fragmentManager = fragmentManager,
                        fragment = HomeOverviewFragment(),
                    )

                    BottomMenuOption.Shop -> FragmentContainer(
                        modifier = Modifier.fillMaxSize(),
                        fragmentManager = fragmentManager,
                        fragment = StoreFragment.newInstance(),
                    )

                    BottomMenuOption.Gem -> TODO()

                    BottomMenuOption.Profile -> if (uiState.airdropLoggedIn) {
                        FragmentContainer(
                            modifier = Modifier.fillMaxSize(),
                            fragmentManager = fragmentManager,
                            fragment = ProfileFragment(),
                        )
                    } else {
                        FragmentContainer(
                            modifier = Modifier.fillMaxSize(),
                            fragmentManager = fragmentManager,
                            fragment = ProfileLoginFragment(),
                        )
                    }

                    BottomMenuOption.Settings -> FragmentContainer(
                        modifier = Modifier.fillMaxSize(),
                        fragmentManager = fragmentManager,
                        fragment = AllSettingsFragment.newInstance(),
                    )
                }
            }

            NavigationMenu(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                selectedItem = uiState.selectedMenuItem,
                onMenuItemClicked = onMenuItemClicked,
            )
        }
    }
}

@Composable
private fun NavigationMenu(
    selectedItem: BottomMenuOption,
    onMenuItemClicked: (BottomMenuOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = TariDesignSystem.colors.componentsNavbarBackground,
        shape = TariDesignSystem.shapes.bottomMenu,
        elevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomNavigationItem(
                iconRes = if (selectedItem == BottomMenuOption.Home) R.drawable.vector_home_nav_menu_home_filled else R.drawable.vector_home_nav_menu_home,
                onItemClick = { onMenuItemClicked(BottomMenuOption.Home) },
            )
            BottomNavigationItem(
                iconRes = if (selectedItem == BottomMenuOption.Shop) R.drawable.vector_home_nav_menu_shop_filled else R.drawable.vector_home_nav_menu_shop,
                onItemClick = { onMenuItemClicked(BottomMenuOption.Shop) },
            )
            // FIXME: uncomment once the gem feature is implemented
//            BottomNavigationItem(
//                iconRes = if (selectedItem == BottomMenuOption.Gem) R.drawable.vector_home_nav_menu_gem_filled else R.drawable.vector_home_nav_menu_gem,
//                onItemClick = { onMenuItemClicked(BottomMenuOption.Gem) },
//            )
            BottomNavigationItem(
                iconRes = if (selectedItem == BottomMenuOption.Profile) R.drawable.vector_home_nav_menu_profile_filled else R.drawable.vector_home_nav_menu_profile,
                onItemClick = { onMenuItemClicked(BottomMenuOption.Profile) },
            )
            BottomNavigationItem(
                iconRes = if (selectedItem == BottomMenuOption.Settings) R.drawable.vector_home_nav_menu_settings_filled else R.drawable.vector_home_nav_menu_settings,
                onItemClick = { onMenuItemClicked(BottomMenuOption.Settings) },
            )
        }
    }
}

@Composable
private fun BottomNavigationItem(
    iconRes: Int,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        IconButton(onClick = onItemClick) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = TariDesignSystem.colors.componentsNavbarIcons,
            )
        }
    }
}

@Composable
private fun FragmentContainer(
    modifier: Modifier = Modifier,
    fragmentManager: FragmentManager,
    fragment: Fragment,
) {
    var initialized by rememberSaveable { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { context -> FragmentContainerView(context).apply { id = FRAGMENT_CONTAINER_ID } },
        update = { view ->
            if (!initialized) {
                fragmentManager.commit {
                    replace(view.id, fragment, fragment.javaClass.simpleName)
                }

                initialized = true
            } else {
                fragmentManager.onContainerAvailable(view)
            }
        }
    )
}

@Composable
@Preview
private fun NavigationMenuPreview() {
    PreviewPrimarySurface(TariTheme.Dark) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray)
        ) {
            NavigationMenu(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                selectedItem = BottomMenuOption.Home,
                onMenuItemClicked = {},
            )
        }
    }
}
