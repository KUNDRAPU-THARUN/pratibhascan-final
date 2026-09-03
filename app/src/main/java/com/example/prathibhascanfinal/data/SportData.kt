package com.example.prathibhascanfinal.data

import com.example.prathibhascanfinal.R

object SportData {

    data class SportDetail(
        val name: String,
        val iconRes: Int,
        val category: String,
        val modules: List<String>,
        val skills: Map<String, List<String>>,
        val description: String,
        val rules: String,
        val equipment: String,
        val drills: List<String>,
        val suggestions: List<String>,
        val formLayoutRes: Int
    )

    private val sports = mapOf(
        "Cricket" to SportDetail(
            name = "Cricket",
            iconRes = R.drawable.ic_outdoor_domain,
            category = "Team Sport",
            modules = listOf("Batting", "Bowling", "Fielding", "Wicket Keeping"),
            skills = mapOf(
                "Batting" to listOf("Cover Drive", "Straight Drive", "Pull Shot", "Hook Shot", "Sweep", "Cut Shot", "Lofted Drive"),
                "Bowling" to listOf("Fast Bowling", "Swing Bowling", "Yorker", "Bouncer", "Off Spin", "Leg Spin"),
                "Fielding" to listOf("Basic Fielding", "High Catch", "Slip Catch")
            ),
            description = "A bat-and-ball game played between two teams of eleven players on a field at the center of which is a 22-yard pitch.",
            rules = "1. Each team bats and bowls.\n2. The objective is to score more runs than the opponent.\n3. Bowlers deliver 6 balls per over.",
            equipment = "Bat, Ball, Pads, Gloves, Helmet, Stumps.",
            drills = listOf("Shadow Batting", "Target Bowling", "High Catching"),
            suggestions = listOf("How to improve my batting stance?", "Exercises for faster bowling?", "Drills for slip catching?"),
            formLayoutRes = R.layout.layout_form_cricket
        ),
        "Football" to SportDetail(
            name = "Football",
            iconRes = R.drawable.ic_outdoor_domain,
            category = "Team Sport",
            modules = listOf("Passing", "Dribbling", "Shooting", "Heading", "Goalkeeping"),
            skills = mapOf(
                "Dribbling" to listOf("Cone Slalom", "Zinedine Roulette", "Step Over", "Elastico"),
                "Shooting" to listOf("Power Strike", "Curled Shot", "Finesse Shot", "Volley"),
                "Passing" to listOf("Short Passing", "Long Ball", "Wall Pass")
            ),
            description = "A team sport played with a spherical ball between two teams of 11 players.",
            rules = "1. No hands allowed except for goalkeepers.\n2. Score by getting the ball into the net.\n3. 90 minutes per match.",
            equipment = "Ball, Cleats, Shin Guards, Goal Nets.",
            drills = listOf("Dribbling Circuit", "Penalty Drills", "Passing Triangle"),
            suggestions = listOf("Dribbling drills for speed?", "How to improve my goal accuracy?", "Passing drills for midfielders?"),
            formLayoutRes = R.layout.layout_form_football
        ),
        "Badminton" to SportDetail(
            name = "Badminton",
            iconRes = R.drawable.ic_indoor_domain,
            category = "Racket Sport",
            modules = listOf("Serve", "Smash", "Drop Shot", "Clear", "Net Shot", "Backhand"),
            skills = mapOf(
                "Serve" to listOf("High Serve", "Low Serve", "Flick Serve"),
                "Smash" to listOf("Power Smash", "Jump Smash", "Slicing Smash"),
                "Net Shot" to listOf("Hairpin Net Shot", "Net Kill", "Cross-court Net Shot"),
                "Clear" to listOf("Attacking Clear", "Defensive Clear"),
                "Backhand" to listOf("Backhand Clear", "Backhand Drop", "Backhand Smash")
            ),
            description = "A racket sport played using rackets to hit a shuttlecock across a net.",
            rules = "1. Played to 21 points.\n2. Serve must be below the waist.\n3. Shuttlecock must not touch the ground.",
            equipment = "Racket, Shuttlecock, Net, Indoor Shoes.",
            drills = listOf("Footwork Drills", "Shuttle Loading", "Wall Practice"),
            suggestions = listOf("How to improve my smash power?", "Exercises for faster footwork?", "Tips for better net control?"),
            formLayoutRes = R.layout.layout_form_racket
        ),
        "Athletics" to SportDetail(
            name = "Athletics",
            iconRes = R.drawable.ic_athletics_domain,
            category = "Athletics",
            modules = listOf("Sprint", "Relay", "Long Jump", "High Jump", "Hurdles", "Javelin"),
            skills = mapOf(
                "Sprint" to listOf("Sprint Start", "Running Form", "Crouch Start", "Arm Swing", "Stride Length"),
                "Long Jump" to listOf("Approach Run", "Take-off", "Flight Phase", "Landing"),
                "Javelin" to listOf("Grip", "Run-up", "Cross-steps", "Release", "Follow Through"),
                "High Jump" to listOf("Approach", "Arching", "Bar Clearance")
            ),
            description = "A collection of sporting events that involve competitive running, jumping, throwing, and walking.",
            rules = "1. Stay in your lane during sprints.\n2. False start leads to disqualification.\n3. Measured from the nearest break in the landing area.",
            equipment = "Spikes, Running Gear, Javelin, Landing Mat.",
            drills = listOf("Block Starts", "Bounding", "Plyometrics"),
            suggestions = listOf("How to improve my sprint start?", "Exercises for better stride length?", "Tips for long jump landing?"),
            formLayoutRes = R.layout.layout_form_athletics
        ),
        "Basketball" to SportDetail(
            name = "Basketball",
            iconRes = R.drawable.ic_outdoor_domain,
            category = "Team Sport",
            modules = listOf("Dribbling", "Passing", "Shooting", "Defense", "Rebounding"),
            skills = mapOf(
                "Shooting" to listOf("Jump Shot", "Free Throw", "Three Pointer", "Layup", "Dunk"),
                "Dribbling" to listOf("Crossover", "Between Legs", "Behind Back", "Power Dribble"),
                "Passing" to listOf("Chest Pass", "Bounce Pass", "Overhead Pass", "Baseball Pass")
            ),
            description = "A game played between two teams of five players each on a rectangular court.",
            rules = "1. Score by throwing ball through a hoop.\n2. Dribble while moving.\n3. Shot clock is 24 seconds.",
            equipment = "Ball, Hoop, Sneakers.",
            drills = listOf("Free Throw Practice", "Layup Drills", "Three-Point Shootout"),
            suggestions = listOf("How to improve my jump shot?", "Dribbling drills for point guards?", "Tips for better rebounding?"),
            formLayoutRes = R.layout.layout_form_basketball
        ),
        "Chess" to SportDetail(
            name = "Chess",
            iconRes = R.drawable.ic_other_sports_domain,
            category = "Indoor / Precision",
            modules = listOf("Opening", "Midgame", "Endgame", "Tactics"),
            skills = mapOf(
                "Opening" to listOf("E4 Theory", "D4 Theory", "Sicilian Defense", "Ruy Lopez"),
                "Endgame" to listOf("King & Pawn", "Rook Endings", "Bishop vs Knight")
            ),
            description = "A board game of strategic skill for two players.",
            rules = "1. Each player starts with 16 pieces.\n2. Goal is to checkmate the opponent's king.",
            equipment = "Chessboard, 32 Chess Pieces.",
            drills = listOf("Puzzle Rush", "Opening Drills", "Endgame Scenarios"),
            suggestions = listOf("Best opening for beginners?", "How to improve tactical vision?"),
            formLayoutRes = R.layout.layout_form_chess
        ),
        "Combat" to SportDetail(
            name = "Combat",
            iconRes = R.drawable.ic_academy_emblem,
            category = "Combat",
            modules = listOf("Stance", "Striking", "Grappling", "Defense"),
            skills = emptyMap(),
            description = "Martial arts and combat sports training.",
            rules = "Follow safety and sportsmanship rules.",
            equipment = "Protective Gear, Gloves.",
            drills = listOf("Shadow Boxing", "Pad Work", "Sparring"),
            suggestions = listOf("How to improve my reaction time?"),
            formLayoutRes = R.layout.layout_form_combat
        )
    )

    fun getDetail(name: String): SportDetail {
        val exactMatch = sports.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
        if (exactMatch != null) return exactMatch

        // Category based defaults
        return when {
            name.contains("Tennis", true) || name.contains("Squash", true) -> getDetail("Badminton").copy(name = name)
            name.contains("Running", true) || name.contains("Jump", true) || name.contains("Throw", true) -> getDetail("Athletics").copy(name = name)
            name.contains("Boxing", true) || name.contains("Wrestling", true) || name.contains("Karate", true) -> getDetail("Combat").copy(name = name)
            else -> SportDetail(
                name = name,
                iconRes = R.drawable.ic_academy_emblem,
                category = "General",
                modules = listOf("Training", "Skill Drills", "Match Simulation"),
                skills = emptyMap(),
                description = "Sports assessment and training arena.",
                rules = "Follow standard sportsmanship rules.",
                equipment = "Standard training gear.",
                drills = listOf("Basic Warmup", "Skill Drill"),
                suggestions = listOf("How to improve my overall stamina?", "What is the best post-workout nutrition?"),
                formLayoutRes = R.layout.layout_form_skill
            )
        }
    }

    fun getAllSportNames(): Array<String> = arrayOf("Cricket", "Football", "Basketball", "Badminton", "Volleyball", "Athletics", "Chess", "Tennis", "Table Tennis", "Sprints", "Running", "Long Jump", "Boxing", "Wrestling", "Karate", "General Training")

    fun getIcon(name: String): Int = getDetail(name).iconRes
    fun getCategory(name: String): String = getDetail(name).category
}
