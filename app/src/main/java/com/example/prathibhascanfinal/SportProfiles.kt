package com.example.prathibhascanfinal

import androidx.room.*

@Entity(tableName = "cricket_profiles")
data class CricketProfile(
    @PrimaryKey val userEmail: String,
    val playingRole: String = "All Rounder",
    val battingStyle: String = "Right Hand",
    val bowlingStyle: String = "Medium",
    val armSpeed: Double = 0.0,
    val batSpeed: Double = 0.0,
    val bowlingAccuracy: Int = 0,
    val highestScore: Int = 0,
    val battingAverage: Double = 0.0,
    val bowlingEconomy: Double = 0.0,
    val reactionTime: Double = 0.0,
    val aiRating: Int = 0
)

@Entity(tableName = "football_profiles")
data class FootballProfile(
    @PrimaryKey val userEmail: String,
    val position: String = "Forward",
    val preferredFoot: String = "Right",
    val passingAccuracy: Int = 0,
    val sprintSpeed: Double = 0.0,
    val ballControl: Int = 0,
    val dribbling: Int = 0,
    val finishing: Int = 0,
    val vision: Int = 0,
    val heading: Int = 0,
    val aiRating: Int = 0
)

@Entity(tableName = "basketball_profiles")
data class BasketballProfile(
    @PrimaryKey val userEmail: String,
    val position: String = "Guard",
    val verticalJump: Double = 0.0,
    val threePointAccuracy: Int = 0,
    val freeThrowPct: Int = 0,
    val dribbling: Int = 0,
    val stealRate: Double = 0.0,
    val reboundAverage: Double = 0.0,
    val aiRating: Int = 0
)

@Entity(tableName = "racket_sport_profiles")
data class RacketSportProfile(
    @PrimaryKey val userEmail: String,
    val sportName: String, // Badminton, Tennis, Table Tennis, Squash
    val playStyle: String = "Singles",
    val gripType: String = "Standard",
    val forehandPower: Int = 0,
    val backhandPower: Int = 0,
    val smashSpeed: Double = 0.0,
    val footworkScore: Int = 0,
    val dropAccuracy: Int = 0,
    val nationalRanking: Int = 0,
    val aiRating: Int = 0
)

@Entity(tableName = "chess_profiles")
data class ChessProfile(
    @PrimaryKey val userEmail: String,
    val fideRating: Int = 0,
    val rapidRating: Int = 0,
    val preferredOpening: String = "",
    val playingStyle: String = "Positional",
    val puzzleRating: Int = 0,
    val chessComId: String = "",
    val lichessId: String = "",
    val aiRating: Int = 0
)

@Entity(tableName = "athletics_profiles")
data class AthleticsProfile(
    @PrimaryKey val userEmail: String,
    val eventType: String, // Track, Field
    val specificEvent: String, // 100m, Long Jump, etc.
    val personalBest: String = "",
    val reactionTime: Double = 0.0,
    val vo2Max: Double = 0.0,
    val topSpeed: Double = 0.0,
    val jumpDistance: Double = 0.0,
    val throwDistance: Double = 0.0,
    val aiRating: Int = 0
)

@Entity(tableName = "swimming_profiles")
data class SwimmingProfile(
    @PrimaryKey val userEmail: String,
    val primaryStroke: String = "Freestyle",
    val poolLength: String = "50m",
    val bestTiming: String = "",
    val strokeEfficiency: Int = 0,
    val aiRating: Int = 0
)

@Entity(tableName = "combat_sport_profiles")
data class CombatSportProfile(
    @PrimaryKey val userEmail: String,
    val sportName: String, // Boxing, Wrestling, Karate, Fencing
    val weightClass: String = "",
    val stance: String = "Orthodox",
    val strikePower: Int = 0,
    val defenseRating: Int = 0,
    val aiRating: Int = 0
)

@Entity(tableName = "skill_sport_profiles")
data class SkillSportProfile(
    @PrimaryKey val userEmail: String,
    val sportName: String, // Yoga, Gymnastics, Mallakhamba, Pilates
    val flexibilityIndex: Int = 0,
    val balanceScore: Int = 0,
    val coreStrength: Int = 0,
    val movementQuality: Int = 0,
    val aiRating: Int = 0
)

@Entity(tableName = "team_sport_profiles")
data class TeamSportProfile(
    @PrimaryKey val userEmail: String,
    val sportName: String, // Kabaddi, Kho-Kho, Hockey, Volleyball, Rugby
    val position: String = "",
    val agilityScore: Int = 0,
    val staminaRating: Int = 0,
    val technicalSkill: Int = 0,
    val aiRating: Int = 0
)

@Entity(tableName = "precision_sport_profiles")
data class PrecisionSportProfile(
    @PrimaryKey val userEmail: String,
    val sportName: String, // Archery, Shooting, Golf, Billiards
    val dominantEye: String = "Right",
    val equipmentType: String = "",
    val accuracyPct: Int = 0,
    val stabilityIndex: Int = 0,
    val aiRating: Int = 0
)

@Dao
interface SportProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCricket(profile: CricketProfile)
    @Query("SELECT * FROM cricket_profiles WHERE userEmail = :email") suspend fun getCricket(email: String): CricketProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveFootball(profile: FootballProfile)
    @Query("SELECT * FROM football_profiles WHERE userEmail = :email") suspend fun getFootball(email: String): FootballProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveBasketball(profile: BasketballProfile)
    @Query("SELECT * FROM basketball_profiles WHERE userEmail = :email") suspend fun getBasketball(email: String): BasketballProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveRacketSport(profile: RacketSportProfile)
    @Query("SELECT * FROM racket_sport_profiles WHERE userEmail = :email AND sportName = :sport") suspend fun getRacketSport(email: String, sport: String): RacketSportProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveChess(profile: ChessProfile)
    @Query("SELECT * FROM chess_profiles WHERE userEmail = :email") suspend fun getChess(email: String): ChessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAthletics(profile: AthleticsProfile)
    @Query("SELECT * FROM athletics_profiles WHERE userEmail = :email") suspend fun getAthletics(email: String): AthleticsProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSwimming(profile: SwimmingProfile)
    @Query("SELECT * FROM swimming_profiles WHERE userEmail = :email") suspend fun getSwimming(email: String): SwimmingProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCombatSport(profile: CombatSportProfile)
    @Query("SELECT * FROM combat_sport_profiles WHERE userEmail = :email AND sportName = :sport") suspend fun getCombatSport(email: String, sport: String): CombatSportProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSkillSport(profile: SkillSportProfile)
    @Query("SELECT * FROM skill_sport_profiles WHERE userEmail = :email AND sportName = :sport") suspend fun getSkillSport(email: String, sport: String): SkillSportProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTeamSport(profile: TeamSportProfile)
    @Query("SELECT * FROM team_sport_profiles WHERE userEmail = :email AND sportName = :sport") suspend fun getTeamSport(email: String, sport: String): TeamSportProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePrecisionSport(profile: PrecisionSportProfile)
    @Query("SELECT * FROM precision_sport_profiles WHERE userEmail = :email AND sportName = :sport") suspend fun getPrecisionSport(email: String, sport: String): PrecisionSportProfile?
}
