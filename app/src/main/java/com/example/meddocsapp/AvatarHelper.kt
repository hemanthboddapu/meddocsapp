package com.example.meddocsapp

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Utility object for selecting patient avatars based on age and gender.
 */
object AvatarHelper {

    /**
     * Get the appropriate avatar drawable resource based on patient gender and date of birth.
     *
     * Age categories:
     * - Infant: 0-2 years
     * - Child: 3-12 years
     * - Teen: 13-19 years
     * - Adult: 20-59 years
     * - Senior: 60+ years
     *
     * @param gender Patient gender ("Male", "Female", "M", "F", etc.)
     * @param dob Patient date of birth string (dd/MM/yyyy format)
     * @return Drawable resource ID for the avatar
     */
    fun getAvatarResource(gender: String?, dob: String?): Int {
        val isMale = isMaleGender(gender)
        val isFemale = isFemaleGender(gender)
        val age = calculateAge(dob)

        return when {
            age != null && age < 3 -> {
                if (isMale) R.drawable.avatar_male_infant
                else if (isFemale) R.drawable.avatar_female_infant
                else R.drawable.avatar_default
            }
            age != null && age < 13 -> {
                if (isMale) R.drawable.avatar_male_child
                else if (isFemale) R.drawable.avatar_female_child
                else R.drawable.avatar_default
            }
            age != null && age < 20 -> {
                if (isMale) R.drawable.avatar_male_teen
                else if (isFemale) R.drawable.avatar_female_teen
                else R.drawable.avatar_default
            }
            age != null && age < 60 -> {
                if (isMale) R.drawable.avatar_male_adult
                else if (isFemale) R.drawable.avatar_female_adult
                else R.drawable.avatar_default
            }
            age != null && age >= 60 -> {
                if (isMale) R.drawable.avatar_male_senior
                else if (isFemale) R.drawable.avatar_female_senior
                else R.drawable.avatar_default
            }
            // Age unknown, use gender only
            isMale -> R.drawable.avatar_male_adult
            isFemale -> R.drawable.avatar_female_adult
            else -> R.drawable.avatar_default
        }
    }

    /**
     * Check if gender indicates male.
     */
    private fun isMaleGender(gender: String?): Boolean {
        val g = gender?.lowercase()?.trim() ?: return false
        return g == "male" || g == "m" || g == "boy" || g == "man"
    }

    /**
     * Check if gender indicates female.
     */
    private fun isFemaleGender(gender: String?): Boolean {
        val g = gender?.lowercase()?.trim() ?: return false
        return g == "female" || g == "f" || g == "girl" || g == "woman"
    }

    /**
     * Calculate age from date of birth string.
     * @param dob Date of birth in dd/MM/yyyy format
     * @return Age in years, or null if unable to parse
     */
    private fun calculateAge(dob: String?): Int? {
        if (dob.isNullOrBlank()) return null

        val trimmed = dob.trim()
        // Try to extract an integer age from the string (handles '2', '02', '2y', '2 years', etc.)
        val numberMatch = Regex("\\b(\\d{1,3})\\b").find(trimmed)
        if (numberMatch != null) {
            val ageStr = numberMatch.groupValues[1]
            try {
                val ageVal = ageStr.toInt()
                if (ageVal in 0..130) return ageVal
            } catch (_: NumberFormatException) {
                // fall through to date parsing
            }
        }

        // Otherwise, parse as a date in dd/MM/yyyy format
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val birthDate = dateFormat.parse(trimmed) ?: return null
            val birthCalendar = Calendar.getInstance().apply { time = birthDate }
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) {
            null
        }
    }
}
