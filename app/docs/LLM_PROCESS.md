Who Wrote What
AI (Gemini & Copilot):** Generated initial Gradle setup, Retrofit/Room boilerplate, JSON parsers, Compose screen layouts, and initial unit/mutation test scaffolding under our SPEC.md and STYLE.md constraints.
Team (Human):Designed the UI layouts with Google Stitch, directed the Clean MVVM architecture, reviewed and refactored generated code, resolved merge and build errors, manually verified mutations and 20 emulator UX scenarios, and wrote docs/REFLECTION.md and docs/LLM_PROCESS.md.

The LLMs we used were primarily Gemini as an agent in the Android Studio IDE. We also utilized some Copilot because our tokens ran out and GitHub provided it for free for students.

The work was split into primarily three sessions.

Session 1, took approximately 3-4 hours:
We asked the AI to generate the correct boilerplate files and structure to support an Android Studio project that uses room, retrofit and kotlinx.serialization. We also decided to go for Open Meteo to access weather data across the globe. The AI created necessary files such as OpenMeteoApi.kt and GeocodingApi.kt so we could have access to the data. Nothing seemed out of the ordinary when we inspected the generated files so we decided to keep the changes. We asked it explicitly to create a singleton Dependency Injection container so only one OkHttpClient session would be held during the process. We also asked it to create classes for parsing the JSON data into actual internal models.

We asked it to create a class to group the weather data so we would have access to an hourly forecast but also a prognosis everyday for the next 6 days. We asked it to generate other components that would fulfill the requirements for the lab, such as implementing geocoding calls.

Then we implemented persistence and we ensured first that the network calls are run on a different thread. We also asked it to finally implement Room database support, and so it produced valid entities that became usable and generally useful during the entire lab, such as CachedForecastEntity.kt and FavouriteLocationEntity.kt. It created Data Access Objects, the repository for the app and even created the database. After inspecting the database we realized it did a good job and we kept it, none of the files seemed out of the ordinary.

We made sure that the code generated would support no internet connection and show cached data and valid errors instead of crashing. We tested every change by running the old tests the ai had created and we found that it didn't give any errors, and the app ran fine.

This is a prompt we fed to the AI (after of course making sure it found the .md files):


“Implement local persistence using Room (WeatherDatabase, DAOs, Entities) to cache fetched forecasts and provide offline fallback, ensuring all network and database operations execute off the main thread.”

Some issues we ran into though and had to fix is that the AI made a typo in the AndroidManifest.xml, where it typed android:android: twice on a permissions tag. This stopped the app from running but removing the duplication fixed the issue.

The AI also accidentally made a mismatch between Room KSP and our kotlin versions, but it was an easy fix as well.

Session 2, 3 hours:
Here we took the professors advice, and using google stitch we generated 20 different UI:s, some for settings screen, home screen, search screen etc. From this we mashed together screens by picking components we liked and used those as reference. We then asked Gemini to set up the apps two different color schemes, a light and dark theme that the user can switch between.

Then we asked the AI to create WeatherUiState.kt to support the loading, success, offline and error events. We also made sure the AI kept the raw weather conversions separated from the ui to not clog up the code.

We asked the AI to use stateflow when making the screens so it automatically updated whenever new data arrived and we made sure the navbar was kept outside of the VM. The navigation between the screens was kept inside of the WeatherNavGraph.kt file with ui callbacks.

By giving the AI the screenshots of the screens we generated with google stitch and also specifying the actual color hex codes we generated the different screens: homescreen, search screen, weather state, the day’s detail, saved locations, settings.

We made sure to follow the requirements for the project and we made sure to make the AI separate Http exceptions from IO exceptions so the offline searches show the cached results from our Room database instead.

This is a prompt that we used:

“Implement WeatherViewModel.kt exposing required StateFlow states and properties, handling startup caching, network monitoring, search/select/favourite/settings actions, distinguishing IOException from HttpException, and strictly omitting NavController.”

After reviewing the code we noticed some changes and additions the AI did that we decided to change. For example the HomeScreen.kt and DayDetailScreen.kt files became way too big, so we told the AI to split them into smaller cleaner files and the AI split them into *Screen.kt files alongside HomeComponents.kt and DayDetailComponents.kt, while moving the remaining formatting helper functions into WeatherFormatters.kt. Another thing is that we realized if we searched for locations quickly, the older searches finished late and could overwrite the new results. We had to add a cancel function for the searches in case the user searched for many things quickly again.

Session 3, 3 hours:

In this session we split the work between testing edge cases and completing the error-handling tests and merging together the code.

When we merged the parallel branches we worked on, specifically in WeatherRepositoryImpl.kt, WeatherRepository.kt, and gradle-wrapper.properties the older MutableStateFlow created a conflict with the new Room DAO persistence. We fixed the errors by asking the AI to inspect the errors and explain what the issue was so we could direct it to fix the errors and keep the correct code that would ensure everything still worked.

But in the same processing of merging together everything, two errors arose: WeatherForecast.getHourlyForDate() was returning an empty list when a day's nested hourly list wasn't populated. This got fixed by making the AI have the time fall back to todayHourly when the date was equal to today's date. An inner fake repository was also found inside WeatherViewModelTest.kt and it still used old Unit return types instead of Result<Unit>, and of course we made sure that got fixed.

This is a prompt that we used:


“I just merged with another branch. There are now duplicate functions in WeatherRepositoryImpl. Can you look through the new code and explain which versions should stay and why. Can you also summarize what the new code does?”

We made sure that we didn't blindly accept the fixes but also understood what it did so we knew how to quality check everything and make sure that errors didn’t cascade into bigger ones.

We also expanded the unit tests around error handling and verified that the gradle debug test passed. We directed the AI to run mutation tests on the code on our core components to make sure our tests were valid and caught any errors that occurred. We also manually verified the key mutations by temporarily breaking the logic and making sure the tests failed like they should.

We ourselves did the 20 different UX test scenarios and made sure everything passed like it should.

A mishap we found towards the end of the session was that a lot of dead code had accumulated in most of our ViewModel files and WeatherViewModel had become a monolith. We’re not entirely sure whether this was caused by a merge where old code came back from a different branch or if we didn’t review the agent's output properly. In the future we will be more aware of similar issues.
