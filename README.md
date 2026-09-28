# 🏋️‍♂️ GymTracking

A high-end, stealth-designed native Android workout tracking application built with **Kotlin** and **Jetpack Compose**. Designed for serious lifters and fitness enthusiasts who want frictionless tracking and actionable, enterprise-grade analytics without annoying distractions or ads.

---

## ✨ Features

### 📊 Enterprise-Grade Analytics & Insights
- **Smart Coach Intelligence**: Algorithmic coach providing actionable text insights on progressive overload, consistency, recovery, and PR streaks.
- **Muscle Group Radar Chart**: Custom Canvas-rendered 2D polygonal spider chart showing training volume balance across all muscle groups.
- **12-Week Activity Heatmap**: GitHub-style grid visualizing training consistency and workout volume over time.
- **Progressive Overload & Estimated 1RM**: Tracks current weight/reps, calculates **Estimated 1RM** using the capped Epley formula, and shows 30-day progression trends (`↑ +%`, `↓ -%`, `→ Plateau`).
- **Bodyweight Exercise Tracking**: Automatically detects bodyweight exercises (e.g. Pull-ups, Push-ups) and tracks progression by max repetitions instead of weight.
- **Neglected Muscle Groups**: Smart warning cards flagging muscle groups not trained in 7+ days with urgency badges.
- **Month vs Month Comparison**: Delta comparison table comparing workout volume, exercises, and PRs between months.
- **All-Time PR Wall**: Showcases personal bests across all exercises with recency badges.

### 📝 Frictionless Logging
- **Smart Defaults**: Automatically pre-fills your last used weight and reps for the selected exercise.
- **Cardio & Strength Support**: Flexible set logging for strength (weight/reps) and cardio (duration/distance).
- **Interactive Set Management**: Edit or delete sets on the fly with safety confirmations.
- **Session Sharing**: Generate high-res graphical workout summary cards with category accents and gold PR highlights.

### 🎨 Stealth Matte Design
- **Distraction-Free AMOLED Palette**: Low-key deep grays (`#121212`) with desaturated slate and teal accents.
- **Smooth Animations**: Page transitions using `AnimatedContent`, sliding nav indicators, and responsive UI components.
- **Adaptive Navigation**: Modern Material 3 `NavigationSuiteScaffold` with 4 clean tabs (Home, Logs, Insights, More).

### 📚 Exercise Library & Progress Photos
- **Exercise Library**: Searchable database with category filtering and GIF/photo demonstration support.
- **Bulk SAF Import**: Import workout libraries or exercise sets directly from external folders via Android Storage Access Framework.
- **Gym Selfies**: Built-in progress photo gallery with full-screen viewer and secure local storage (powered by Coil `AsyncImage`).
- **Body Weight Tracker**: Canvas-drawn smooth trend line chart tracking weight over time.
- **Data Portability**: Full CSV export with proper comma escaping and complete cardio metrics.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 1.9+
- **UI Toolkit**: Jetpack Compose, Material 3
- **Architecture**: MVVM (Model-View-ViewModel) + Single Activity Architecture
- **Local Database**: Room SQLite (Entity-DAO pattern)
- **Image Pipeline**: Coil 2.x (GIFs, images, async decoding)
- **Asynchronous / Coroutines**: Kotlin Coroutines & Flow
- **Graphics**: Compose Canvas 2D custom drawing

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog / Iguana / Ladybug or newer
- JDK 17
- Android SDK 34+ (minSdk 26, targetSdk 34)

### Build & Run
```bash
# Clone the repository
git clone https://github.com/Jayu9493/GymTracking.git

# Open the project directory
cd GymTracking

# Build debug APK
./gradlew assembleDebug

# Install on connected Android device
./gradlew installDebug
```

---

## 📄 License
This project is licensed under the MIT License.
