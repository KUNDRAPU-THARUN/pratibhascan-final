package com.example.prathibhascanfinal

object SportsPositionHelper {

    fun getPositionsForSport(sportName: String): List<String> {
        val s = sportName.lowercase().trim()
        return when {
            s.contains("cricket") -> listOf(
                "Batsman",
                "Fast Bowler",
                "Spin Bowler",
                "All-Rounder",
                "Wicket Keeper",
                "Opening Batsman",
                "Middle Order Batsman"
            )
            s.contains("football") || s.contains("soccer") -> listOf(
                "Goalkeeper",
                "Center Back",
                "Full Back",
                "Left Back",
                "Right Back",
                "Defensive Midfielder",
                "Central Midfielder",
                "Attacking Midfielder",
                "Winger",
                "Forward",
                "Striker"
            )
            s.contains("basketball") -> listOf(
                "Point Guard",
                "Shooting Guard",
                "Small Forward",
                "Power Forward",
                "Center"
            )
            s.contains("volleyball") -> listOf(
                "Setter",
                "Libero",
                "Middle Blocker",
                "Outside Hitter",
                "Opposite Hitter"
            )
            s.contains("hockey") -> listOf(
                "Goalkeeper",
                "Defender",
                "Sweeper",
                "Midfielder",
                "Forward",
                "Inside Forward"
            )
            s.contains("athletics") || s.contains("track") -> listOf(
                "100m Sprint",
                "200m Sprint",
                "400m Sprint",
                "800m Race",
                "1500m Race",
                "Long Jump",
                "High Jump",
                "Shot Put",
                "Javelin Throw",
                "Discus Throw",
                "Relay"
            )
            s.contains("badminton") || s.contains("tennis") || s.contains("table tennis") -> listOf(
                "Singles Player",
                "Doubles Player",
                "Mixed Doubles Player"
            )
            s.contains("swimming") -> listOf(
                "Freestyle",
                "Backstroke",
                "Breaststroke",
                "Butterfly",
                "Individual Medley"
            )
            s.contains("kabaddi") -> listOf(
                "Raider",
                "Corner Defender",
                "Cover Defender",
                "All-Rounder"
            )
            else -> listOf(
                "Player",
                "Captain",
                "Forward",
                "Defender",
                "Midfielder",
                "Athlete"
            )
        }
    }

    fun getAgeGroups(): List<String> {
        return listOf("Under-12", "Under-14", "Under-16", "Under-17", "Under-19", "Under-21", "Senior", "Open Category")
    }

    fun getTeamTypes(): List<String> {
        return listOf("School Team", "College Team", "House Team", "Class Team", "Practice Group", "Development Squad")
    }

    fun getGenders(): List<String> {
        return listOf("Male", "Female", "Mixed")
    }
}
