/*
 * Copyright 2023 Sasikanth Miriyampalli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.sasikanth.rss.reader

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.defaultComponentContext
import dev.sasikanth.rss.reader.app.App
import dev.sasikanth.rss.reader.di.ApplicationComponent
import dev.sasikanth.rss.reader.di.scopes.ActivityScope
import dev.sasikanth.rss.reader.repository.BrowserType
import dev.sasikanth.rss.reader.repository.BrowserType.Default
import dev.sasikanth.rss.reader.repository.BrowserType.InApp
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT

    val activityComponent = ActivityComponent::class.create(activity = this)

    setContent { activityComponent.app(::openLink) }
  }

  private fun openLink(url: String, browserType: BrowserType) {
    when (browserType) {
      Default -> {
        val intent =
          Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        if (intent.resolveActivity(packageManager) != null) {
          startActivity(intent)
        } else {
          openCustomTab(url)
        }
      }
      InApp -> {
        openCustomTab(url)
      }
    }
  }

  private fun openCustomTab(url: String) {
    val intent = CustomTabsIntent.Builder().build()
    intent.launchUrl(this, url.toUri())
  }
}

fun ApplicationComponent.Companion.from(activity: Activity) =
  (activity.applicationContext as ReaderApplication).appComponent

@Component
@ActivityScope
abstract class ActivityComponent(
  @get:Provides val activity: ComponentActivity,
  @get:Provides val componentContext: ComponentContext = activity.defaultComponentContext(),
  @Component val applicationComponent: ApplicationComponent = ApplicationComponent.from(activity)
) {

  abstract val app: App
}
