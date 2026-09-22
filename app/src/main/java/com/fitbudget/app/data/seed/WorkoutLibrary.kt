package com.fitbudget.app.data.seed

import com.fitbudget.app.domain.model.WorkoutCategory

/** One exercise inside a template. Either rep-based or time-based (durationSeconds != null). */
data class Exercise(
    val name: String,
    val sets: Int,
    val repsLabel: String,
    val restSeconds: Int,
    val instructions: String,
    val durationSeconds: Int? = null
) {
    val isTimed: Boolean get() = durationSeconds != null
}

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val category: WorkoutCategory,
    val description: String,
    val estimatedMinutes: Int,
    /** Rough energy estimate for a ~85 kg beginner; shown as an estimate only. */
    val estimatedCalories: Int,
    val equipment: String,
    val exercises: List<Exercise>
) {
    val totalSets: Int get() = exercises.sumOf { it.sets }
}

/**
 * Beginner-friendly, equipment-free workouts. Bundled with the app so the workout module is
 * fully offline.
 */
object WorkoutLibrary {

    val templates: List<WorkoutTemplate> = listOf(
        WorkoutTemplate(
            id = "full_body_starter",
            name = "Full Body Starter",
            category = WorkoutCategory.FULL_BODY,
            description = "The classic beginner circuit. Trains legs, chest, core and hips.",
            estimatedMinutes = 25,
            estimatedCalories = 180,
            equipment = "None",
            exercises = listOf(
                Exercise(
                    "Bodyweight Squats", 3, "12 reps", 60,
                    "Feet shoulder width, chest up. Sit back as if reaching for a chair, then drive through the heels."
                ),
                Exercise(
                    "Push-ups", 3, "8 reps", 60,
                    "Hands under the shoulders, body in one line. Drop the knees to the floor if 8 full reps are too many."
                ),
                Exercise(
                    "Lunges", 3, "10 reps per leg", 60,
                    "Step forward, lower until the back knee is just above the floor, push back up. Keep the torso tall."
                ),
                Exercise(
                    "Plank", 3, "30 seconds", 45,
                    "Elbows under the shoulders, squeeze the glutes, do not let the hips sag.",
                    durationSeconds = 30
                ),
                Exercise(
                    "Glute Bridge", 3, "15 reps", 45,
                    "Lie on your back, feet flat, lift the hips until the body is straight, squeeze at the top."
                )
            )
        ),
        WorkoutTemplate(
            id = "home_no_equipment",
            name = "Small Room Home Workout",
            category = WorkoutCategory.HOME_WORKOUT,
            description = "Low noise, no jumping, fits in a 2x2 metre space.",
            estimatedMinutes = 20,
            estimatedCalories = 150,
            equipment = "None",
            exercises = listOf(
                Exercise(
                    "Marching in Place", 1, "3 minutes", 30,
                    "Lift the knees to hip height, swing the arms. This is your warm-up.",
                    durationSeconds = 180
                ),
                Exercise(
                    "Chair Squats", 3, "12 reps", 45,
                    "Touch the chair lightly with your hips, then stand back up without using your hands."
                ),
                Exercise(
                    "Wall Push-ups", 3, "12 reps", 45,
                    "Stand an arm's length from the wall, lower the chest towards it, press back."
                ),
                Exercise(
                    "Standing Knee Raises", 3, "20 reps", 40,
                    "Bring the knee up towards the opposite elbow. Controlled, not rushed."
                ),
                Exercise(
                    "Wall Sit", 3, "30 seconds", 45,
                    "Back flat on the wall, thighs parallel to the floor, breathe normally.",
                    durationSeconds = 30
                )
            )
        ),
        WorkoutTemplate(
            id = "strength_basics",
            name = "Strength Basics",
            category = WorkoutCategory.STRENGTH,
            description = "Keeps muscle while you lose fat. Slow reps, longer rests.",
            estimatedMinutes = 30,
            estimatedCalories = 200,
            equipment = "A filled backpack (optional)",
            exercises = listOf(
                Exercise(
                    "Backpack Goblet Squat", 4, "10 reps", 75,
                    "Hold a loaded backpack at your chest. Descend for 3 seconds, stand up in 1."
                ),
                Exercise(
                    "Incline Push-ups", 4, "10 reps", 75,
                    "Hands on a bed or table edge. The higher the surface, the easier the rep."
                ),
                Exercise(
                    "Backpack Row", 4, "12 reps per side", 60,
                    "Hinge at the hips, pull the bag to your ribs, squeeze the shoulder blade."
                ),
                Exercise(
                    "Romanian Deadlift", 3, "12 reps", 60,
                    "Push the hips back with a flat back until you feel the hamstrings, then stand tall."
                ),
                Exercise(
                    "Superman Hold", 3, "20 seconds", 45,
                    "Face down, lift the chest and thighs off the floor, look at the ground.",
                    durationSeconds = 20
                )
            )
        ),
        WorkoutTemplate(
            id = "core_focus",
            name = "Core Focus",
            category = WorkoutCategory.STRENGTH,
            description = "Ten minutes for the midsection. Quality over speed.",
            estimatedMinutes = 12,
            estimatedCalories = 80,
            equipment = "None",
            exercises = listOf(
                Exercise(
                    "Dead Bug", 3, "10 reps per side", 40,
                    "On your back, press the lower back into the floor, extend the opposite arm and leg."
                ),
                Exercise(
                    "Side Plank", 3, "20 seconds per side", 40,
                    "Stack the feet, lift the hips, keep the body in one straight line.",
                    durationSeconds = 20
                ),
                Exercise(
                    "Leg Raises", 3, "12 reps", 40,
                    "Hands under the hips, lower the legs only as far as you can without arching."
                ),
                Exercise(
                    "Bird Dog", 3, "10 reps per side", 40,
                    "From all fours, extend the opposite arm and leg, pause, return with control."
                )
            )
        ),
        WorkoutTemplate(
            id = "walk_steady_30",
            name = "Steady Walk — 30 min",
            category = WorkoutCategory.WALKING,
            description = "The most sustainable fat-loss tool there is. Brisk but conversational.",
            estimatedMinutes = 30,
            estimatedCalories = 160,
            equipment = "Shoes",
            exercises = listOf(
                Exercise(
                    "Easy Warm-up Walk", 1, "5 minutes", 0,
                    "Relaxed pace to loosen up the ankles and hips.",
                    durationSeconds = 300
                ),
                Exercise(
                    "Brisk Walk", 1, "20 minutes", 0,
                    "Fast enough that talking takes effort, slow enough that you can still talk.",
                    durationSeconds = 1200
                ),
                Exercise(
                    "Cool-down Walk", 1, "5 minutes", 0,
                    "Slow it down and let the breathing settle.",
                    durationSeconds = 300
                )
            )
        ),
        WorkoutTemplate(
            id = "walk_intervals_45",
            name = "Walk Intervals — 45 min",
            category = WorkoutCategory.WALKING,
            description = "Alternating fast and easy blocks. Burns more without running.",
            estimatedMinutes = 45,
            estimatedCalories = 260,
            equipment = "Shoes",
            exercises = listOf(
                Exercise(
                    "Warm-up Walk", 1, "5 minutes", 0,
                    "Easy pace.", durationSeconds = 300
                ),
                Exercise(
                    "Fast Block", 5, "2 minutes", 120,
                    "Push the pace, arms driving. Rest block is 2 minutes of easy walking.",
                    durationSeconds = 120
                ),
                Exercise(
                    "Steady Finish", 1, "15 minutes", 0,
                    "Back to a comfortable brisk pace.", durationSeconds = 900
                ),
                Exercise(
                    "Cool-down Walk", 1, "5 minutes", 0,
                    "Slow and easy.", durationSeconds = 300
                )
            )
        ),
        WorkoutTemplate(
            id = "mobility_reset",
            name = "Mobility Reset",
            category = WorkoutCategory.MOBILITY,
            description = "For stiff mornings and long sitting days. Never force a stretch.",
            estimatedMinutes = 10,
            estimatedCalories = 40,
            equipment = "None",
            exercises = listOf(
                Exercise(
                    "Neck Rolls", 2, "30 seconds", 20,
                    "Slow half circles, chin to chest. Stop if anything pinches.",
                    durationSeconds = 30
                ),
                Exercise(
                    "Cat-Cow", 2, "10 reps", 20,
                    "On all fours, alternate arching and rounding the spine with the breath."
                ),
                Exercise(
                    "Hip Circles", 2, "10 reps per side", 20,
                    "Stand tall, draw big slow circles with one knee."
                ),
                Exercise(
                    "Standing Hamstring Stretch", 2, "30 seconds per side", 20,
                    "Heel forward, hinge from the hips, back flat.",
                    durationSeconds = 30
                ),
                Exercise(
                    "Child's Pose", 1, "45 seconds", 0,
                    "Hips to heels, arms long, breathe into the back.",
                    durationSeconds = 45
                )
            )
        ),
        WorkoutTemplate(
            id = "full_body_progress",
            name = "Full Body — Level 2",
            category = WorkoutCategory.FULL_BODY,
            description = "Move here once Full Body Starter feels easy for two weeks.",
            estimatedMinutes = 35,
            estimatedCalories = 250,
            equipment = "A filled backpack (optional)",
            exercises = listOf(
                Exercise(
                    "Squat Pulse", 4, "15 reps", 60,
                    "Squat down, pulse twice at the bottom, stand up. That is one rep."
                ),
                Exercise(
                    "Push-ups", 4, "12 reps", 60,
                    "Full range: chest close to the floor, elbows about 45° from the body."
                ),
                Exercise(
                    "Reverse Lunge", 4, "12 reps per leg", 60,
                    "Step back instead of forward - easier on the knees, same benefit."
                ),
                Exercise(
                    "Pike Push-ups", 3, "8 reps", 60,
                    "Hips high, head between the hands. This is the shoulder builder."
                ),
                Exercise(
                    "Plank Shoulder Taps", 3, "20 taps", 45,
                    "In a plank, tap the opposite shoulder without letting the hips rock."
                ),
                Exercise(
                    "Plank", 3, "45 seconds", 45,
                    "Hold tight and breathe.", durationSeconds = 45
                )
            )
        )
    )

    fun byId(id: String): WorkoutTemplate? = templates.firstOrNull { it.id == id }

    fun byCategory(category: WorkoutCategory): List<WorkoutTemplate> =
        templates.filter { it.category == category }
}
