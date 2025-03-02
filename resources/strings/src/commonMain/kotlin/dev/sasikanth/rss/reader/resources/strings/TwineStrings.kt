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
package dev.sasikanth.rss.reader.resources.strings

data class TwineStrings(
  val appName: String,
  val postSourceUnknown: String,
  val buttonAll: String,
  val buttonAddFeed: String,
  val buttonGoBack: String,
  val buttonCancel: String,
  val buttonAdd: String,
  val buttonChange: String,
  val feedEntryHint: String,
  val share: String,
  val scrollToTop: String,
  val noFeeds: String,
  val swipeUpGetStarted: String,
  val feedNameHint: String,
  val editFeedName: String,
  val errorUnsupportedFeed: String,
  val errorMalformedXml: String,
  val errorRequestTimeout: String,
  val errorFeedNotFound: (Int) -> String,
  val errorServer: (Int) -> String,
  val errorTooManyRedirects: String,
  val errorUnAuthorized: (Int) -> String,
  val errorUnknownHttpStatus: (Int) -> String,
  val searchHint: String,
  val searchSortNewest: String,
  val searchSortNewestFirst: String,
  val searchSortOldest: String,
  val searchSortOldestFirst: String,
  val bookmark: String,
  val bookmarks: String,
  val settings: String,
  val moreMenuOptions: String,
  val settingsBrowserTypeTitle: String,
  val settingsBrowserTypeSubtitle: String,
  val feeds: String,
  val editFeeds: String,
  val comments: String
)

object Locales {
  const val EN = "en"
}

expect fun String.fmt(vararg args: Any?): String
