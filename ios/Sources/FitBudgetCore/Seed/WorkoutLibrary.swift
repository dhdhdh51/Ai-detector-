import Foundation

/// One exercise inside a template. Either rep-based or time-based (`durationSeconds != nil`).
public struct Exercise: Identifiable, Equatable, Sendable {
    public var id: String { name }

    public let name: String
    public let sets: Int
    public let repsLabel: String
    public let restSeconds: Int
    public let instructions: String
    public let durationSeconds: Int?

    public init(
        name: String,
        sets: Int,
        repsLabel: String,
        restSeconds: Int,
        instructions: String,
        durationSeconds: Int? = nil
    ) {
        self.name = name
        self.sets = sets
        self.repsLabel = repsLabel
        self.restSeconds = restSeconds
        self.instructions = instructions
        self.durationSeconds = durationSeconds
    }

    public var isTimed: Bool { durationSeconds != nil }
}

public struct WorkoutTemplate: Identifiable, Equatable, Sendable {
    public let id: String
    public let name: String
    public let category: WorkoutCategory
    public let detail: String
    public let estimatedMinutes: Int
    /// Rough energy estimate for a ~85 kg beginner; shown as an estimate only.
    public let estimatedCalories: Int
    public let equipment: String
    public let exercises: [Exercise]

    public init(
        id: String,
        name: String,
        category: WorkoutCategory,
        detail: String,
        estimatedMinutes: Int,
        estimatedCalories: Int,
        equipment: String,
        exercises: [Exercise]
    ) {
        self.id = id
        self.name = name
        self.category = category
        self.detail = detail
        self.estimatedMinutes = estimatedMinutes
        self.estimatedCalories = estimatedCalories
        self.equipment = equipment
        self.exercises = exercises
    }

    public var totalSets: Int { exercises.reduce(0) { $0 + $1.sets } }
}

/// Beginner-friendly, equipment-free workouts bundled with the app so the module is fully offline.
public enum WorkoutLibrary {

    public static let templates: [WorkoutTemplate] = [
        fullBodyStarter, homeNoEquipment, strengthBasics, coreFocus,
        walkSteady30, walkIntervals45, mobilityReset, fullBodyProgress
    ]

    public static func template(id: String) -> WorkoutTemplate? {
        templates.first { $0.id == id }
    }

    public static func templates(category: WorkoutCategory) -> [WorkoutTemplate] {
        templates.filter { $0.category == category }
    }

    // MARK: - Templates

    private static let fullBodyStarter = WorkoutTemplate(
        id: "full_body_starter",
        name: "Full Body Starter",
        category: .fullBody,
        detail: "The classic beginner circuit. Trains legs, chest, core and hips.",
        estimatedMinutes: 25,
        estimatedCalories: 180,
        equipment: "None",
        exercises: [
            Exercise(name: "Bodyweight Squats", sets: 3, repsLabel: "12 reps", restSeconds: 60,
                     instructions: "Feet shoulder width, chest up. Sit back as if reaching for a chair, then drive through the heels."),
            Exercise(name: "Push-ups", sets: 3, repsLabel: "8 reps", restSeconds: 60,
                     instructions: "Hands under the shoulders, body in one line. Drop the knees to the floor if 8 full reps are too many."),
            Exercise(name: "Lunges", sets: 3, repsLabel: "10 reps per leg", restSeconds: 60,
                     instructions: "Step forward, lower until the back knee is just above the floor, push back up. Keep the torso tall."),
            Exercise(name: "Plank", sets: 3, repsLabel: "30 seconds", restSeconds: 45,
                     instructions: "Elbows under the shoulders, squeeze the glutes, do not let the hips sag.",
                     durationSeconds: 30),
            Exercise(name: "Glute Bridge", sets: 3, repsLabel: "15 reps", restSeconds: 45,
                     instructions: "Lie on your back, feet flat, lift the hips until the body is straight, squeeze at the top.")
        ]
    )

    private static let homeNoEquipment = WorkoutTemplate(
        id: "home_no_equipment",
        name: "Small Room Home Workout",
        category: .homeWorkout,
        detail: "Low noise, no jumping, fits in a 2x2 metre space.",
        estimatedMinutes: 20,
        estimatedCalories: 150,
        equipment: "None",
        exercises: [
            Exercise(name: "Marching in Place", sets: 1, repsLabel: "3 minutes", restSeconds: 30,
                     instructions: "Lift the knees to hip height, swing the arms. This is your warm-up.",
                     durationSeconds: 180),
            Exercise(name: "Chair Squats", sets: 3, repsLabel: "12 reps", restSeconds: 45,
                     instructions: "Touch the chair lightly with your hips, then stand back up without using your hands."),
            Exercise(name: "Wall Push-ups", sets: 3, repsLabel: "12 reps", restSeconds: 45,
                     instructions: "Stand an arm's length from the wall, lower the chest towards it, press back."),
            Exercise(name: "Standing Knee Raises", sets: 3, repsLabel: "20 reps", restSeconds: 40,
                     instructions: "Bring the knee up towards the opposite elbow. Controlled, not rushed."),
            Exercise(name: "Wall Sit", sets: 3, repsLabel: "30 seconds", restSeconds: 45,
                     instructions: "Back flat on the wall, thighs parallel to the floor, breathe normally.",
                     durationSeconds: 30)
        ]
    )

    private static let strengthBasics = WorkoutTemplate(
        id: "strength_basics",
        name: "Strength Basics",
        category: .strength,
        detail: "Keeps muscle while you lose fat. Slow reps, longer rests.",
        estimatedMinutes: 30,
        estimatedCalories: 200,
        equipment: "A filled backpack (optional)",
        exercises: [
            Exercise(name: "Backpack Goblet Squat", sets: 4, repsLabel: "10 reps", restSeconds: 75,
                     instructions: "Hold a loaded backpack at your chest. Descend for 3 seconds, stand up in 1."),
            Exercise(name: "Incline Push-ups", sets: 4, repsLabel: "10 reps", restSeconds: 75,
                     instructions: "Hands on a bed or table edge. The higher the surface, the easier the rep."),
            Exercise(name: "Backpack Row", sets: 4, repsLabel: "12 reps per side", restSeconds: 60,
                     instructions: "Hinge at the hips, pull the bag to your ribs, squeeze the shoulder blade."),
            Exercise(name: "Romanian Deadlift", sets: 3, repsLabel: "12 reps", restSeconds: 60,
                     instructions: "Push the hips back with a flat back until you feel the hamstrings, then stand tall."),
            Exercise(name: "Superman Hold", sets: 3, repsLabel: "20 seconds", restSeconds: 45,
                     instructions: "Face down, lift the chest and thighs off the floor, look at the ground.",
                     durationSeconds: 20)
        ]
    )

    private static let coreFocus = WorkoutTemplate(
        id: "core_focus",
        name: "Core Focus",
        category: .strength,
        detail: "Ten minutes for the midsection. Quality over speed.",
        estimatedMinutes: 12,
        estimatedCalories: 80,
        equipment: "None",
        exercises: [
            Exercise(name: "Dead Bug", sets: 3, repsLabel: "10 reps per side", restSeconds: 40,
                     instructions: "On your back, press the lower back into the floor, extend the opposite arm and leg."),
            Exercise(name: "Side Plank", sets: 3, repsLabel: "20 seconds per side", restSeconds: 40,
                     instructions: "Stack the feet, lift the hips, keep the body in one straight line.",
                     durationSeconds: 20),
            Exercise(name: "Leg Raises", sets: 3, repsLabel: "12 reps", restSeconds: 40,
                     instructions: "Hands under the hips, lower the legs only as far as you can without arching."),
            Exercise(name: "Bird Dog", sets: 3, repsLabel: "10 reps per side", restSeconds: 40,
                     instructions: "From all fours, extend the opposite arm and leg, pause, return with control.")
        ]
    )

    private static let walkSteady30 = WorkoutTemplate(
        id: "walk_steady_30",
        name: "Steady Walk — 30 min",
        category: .walking,
        detail: "The most sustainable fat-loss tool there is. Brisk but conversational.",
        estimatedMinutes: 30,
        estimatedCalories: 160,
        equipment: "Shoes",
        exercises: [
            Exercise(name: "Easy Warm-up Walk", sets: 1, repsLabel: "5 minutes", restSeconds: 0,
                     instructions: "Relaxed pace to loosen up the ankles and hips.", durationSeconds: 300),
            Exercise(name: "Brisk Walk", sets: 1, repsLabel: "20 minutes", restSeconds: 0,
                     instructions: "Fast enough that talking takes effort, slow enough that you can still talk.",
                     durationSeconds: 1200),
            Exercise(name: "Cool-down Walk", sets: 1, repsLabel: "5 minutes", restSeconds: 0,
                     instructions: "Slow it down and let the breathing settle.", durationSeconds: 300)
        ]
    )

    private static let walkIntervals45 = WorkoutTemplate(
        id: "walk_intervals_45",
        name: "Walk Intervals — 45 min",
        category: .walking,
        detail: "Alternating fast and easy blocks. Burns more without running.",
        estimatedMinutes: 45,
        estimatedCalories: 260,
        equipment: "Shoes",
        exercises: [
            Exercise(name: "Warm-up Walk", sets: 1, repsLabel: "5 minutes", restSeconds: 0,
                     instructions: "Easy pace.", durationSeconds: 300),
            Exercise(name: "Fast Block", sets: 5, repsLabel: "2 minutes", restSeconds: 120,
                     instructions: "Push the pace, arms driving. Rest block is 2 minutes of easy walking.",
                     durationSeconds: 120),
            Exercise(name: "Steady Finish", sets: 1, repsLabel: "15 minutes", restSeconds: 0,
                     instructions: "Back to a comfortable brisk pace.", durationSeconds: 900),
            Exercise(name: "Cool-down Walk", sets: 1, repsLabel: "5 minutes", restSeconds: 0,
                     instructions: "Slow and easy.", durationSeconds: 300)
        ]
    )

    private static let mobilityReset = WorkoutTemplate(
        id: "mobility_reset",
        name: "Mobility Reset",
        category: .mobility,
        detail: "For stiff mornings and long sitting days. Never force a stretch.",
        estimatedMinutes: 10,
        estimatedCalories: 40,
        equipment: "None",
        exercises: [
            Exercise(name: "Neck Rolls", sets: 2, repsLabel: "30 seconds", restSeconds: 20,
                     instructions: "Slow half circles, chin to chest. Stop if anything pinches.",
                     durationSeconds: 30),
            Exercise(name: "Cat-Cow", sets: 2, repsLabel: "10 reps", restSeconds: 20,
                     instructions: "On all fours, alternate arching and rounding the spine with the breath."),
            Exercise(name: "Hip Circles", sets: 2, repsLabel: "10 reps per side", restSeconds: 20,
                     instructions: "Stand tall, draw big slow circles with one knee."),
            Exercise(name: "Standing Hamstring Stretch", sets: 2, repsLabel: "30 seconds per side",
                     restSeconds: 20, instructions: "Heel forward, hinge from the hips, back flat.",
                     durationSeconds: 30),
            Exercise(name: "Child's Pose", sets: 1, repsLabel: "45 seconds", restSeconds: 0,
                     instructions: "Hips to heels, arms long, breathe into the back.", durationSeconds: 45)
        ]
    )

    private static let fullBodyProgress = WorkoutTemplate(
        id: "full_body_progress",
        name: "Full Body — Level 2",
        category: .fullBody,
        detail: "Move here once Full Body Starter feels easy for two weeks.",
        estimatedMinutes: 35,
        estimatedCalories: 250,
        equipment: "A filled backpack (optional)",
        exercises: [
            Exercise(name: "Squat Pulse", sets: 4, repsLabel: "15 reps", restSeconds: 60,
                     instructions: "Squat down, pulse twice at the bottom, stand up. That is one rep."),
            Exercise(name: "Push-ups", sets: 4, repsLabel: "12 reps", restSeconds: 60,
                     instructions: "Full range: chest close to the floor, elbows about 45° from the body."),
            Exercise(name: "Reverse Lunge", sets: 4, repsLabel: "12 reps per leg", restSeconds: 60,
                     instructions: "Step back instead of forward - easier on the knees, same benefit."),
            Exercise(name: "Pike Push-ups", sets: 3, repsLabel: "8 reps", restSeconds: 60,
                     instructions: "Hips high, head between the hands. This is the shoulder builder."),
            Exercise(name: "Plank Shoulder Taps", sets: 3, repsLabel: "20 taps", restSeconds: 45,
                     instructions: "In a plank, tap the opposite shoulder without letting the hips rock."),
            Exercise(name: "Plank", sets: 3, repsLabel: "45 seconds", restSeconds: 45,
                     instructions: "Hold tight and breathe.", durationSeconds: 45)
        ]
    )
}
