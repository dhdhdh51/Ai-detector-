import SwiftUI
import FitBudgetCore

// MARK: - Workout list

struct WorkoutListView: View {

    @Environment(AppStore.self) private var store
    @Environment(Router.self) private var router

    @State private var category: WorkoutCategory?

    private var templates: [WorkoutTemplate] {
        guard let category else { return WorkoutLibrary.templates }
        return WorkoutLibrary.templates(category: category)
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: FitTheme.sectionSpacing) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Workouts").font(.title2.bold())
                    Text(
                        todayCount > 0
                            ? "\(todayCount) session\(todayCount == 1 ? "" : "s") done today"
                            : "No session logged today yet"
                    )
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 4)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(WorkoutCategory.allCases, id: \.self) { item in
                            Button {
                                category = (category == item) ? nil : item
                            } label: {
                                Text(item.label)
                                    .font(.caption.weight(.semibold))
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(
                                        category == item
                                            ? FitTheme.brand.opacity(0.18)
                                            : FitTheme.subtleCard
                                    )
                                    .foregroundStyle(category == item ? FitTheme.brand : Color.primary)
                                    .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                ForEach(templates) { template in
                    templateCard(template)
                }

                SectionHeader(
                    title: "History",
                    subtitle: store.workoutHistory.isEmpty
                        ? nil
                        : "\(store.workoutHistory.count) sessions · \(totalMinutes) minutes total"
                )
                .padding(.top, 8)

                if store.workoutHistory.isEmpty {
                    EmptyStateView(
                        systemImage: "figure.strengthtraining.traditional",
                        title: "No workouts finished yet",
                        message: "Finish your first session and it will appear here."
                    )
                    .fitCard()
                }

                ForEach(store.workoutHistory) { session in
                    historyRow(session)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Workout")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var todayCount: Int { store.todaySummary.workoutsCompleted }

    private var totalMinutes: Int {
        store.workoutHistory.reduce(0) { $0 + $1.durationSeconds } / 60
    }

    private func templateCard(_ template: WorkoutTemplate) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(template.name).font(.headline)
                    Text(template.category.label)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(FitTheme.workout)
                }
                Spacer(minLength: 8)
                Button {
                    router.push(.workoutSession(templateID: template.id), on: .workout)
                } label: {
                    Label("Start", systemImage: "play.fill").font(.subheadline.weight(.semibold))
                }
                .buttonStyle(.borderedProminent)
            }

            Text(template.detail).font(.subheadline).foregroundStyle(.secondary)

            Text(
                "\(template.exercises.count) exercises · \(template.totalSets) sets · "
                + "\(template.estimatedMinutes) min · ≈ \(template.estimatedCalories) kcal"
            )
            .font(.caption.weight(.medium))

            Text("Equipment: \(template.equipment)")
                .font(.caption2)
                .foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: 2) {
                ForEach(template.exercises.prefix(3)) { exercise in
                    Text("• \(exercise.name) — \(exercise.sets) × \(exercise.repsLabel)")
                        .font(.caption)
                }
                if template.exercises.count > 3 {
                    Text("• +\(template.exercises.count - 3) more")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .fitCard()
    }

    private func historyRow(_ session: WorkoutSessionRecord) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(session.templateName).font(.subheadline.weight(.semibold))
                Text(
                    "\(Formatters.relativeDay(DayKey(session.day), today: store.todayKey)) · "
                    + "\(Formatters.duration(session.durationSeconds)) · "
                    + "\(session.exercisesCompleted)/\(session.exercisesTotal) exercises"
                )
                .font(.caption2)
                .foregroundStyle(.secondary)
                Text("≈ \(session.estimatedCalories) kcal (estimate)")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            Button(role: .destructive) {
                store.delete(session)
            } label: {
                Image(systemName: "trash")
            }
            .buttonStyle(.borderless)
        }
        .fitCard(padding: 14)
    }
}

// MARK: - Live session

private enum ExerciseStatus {
    case pending, done, skipped
}

struct WorkoutSessionView: View {

    let templateID: String

    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var statuses: [ExerciseStatus] = []
    @State private var currentIndex = 0
    @State private var elapsedSeconds = 0
    @State private var exerciseTimer: Int?
    @State private var isRunning = true
    @State private var startedAt = Date()
    @State private var showExitConfirm = false

    private var template: WorkoutTemplate? { WorkoutLibrary.template(id: templateID) }

    private var completedCount: Int { statuses.filter { $0 == .done }.count }
    private var skippedCount: Int { statuses.filter { $0 == .skipped }.count }
    private var handledCount: Int { statuses.filter { $0 != .pending }.count }
    private var allHandled: Bool { !statuses.isEmpty && !statuses.contains(.pending) }

    var body: some View {
        Group {
            if let template {
                content(template)
            } else {
                VStack(spacing: 12) {
                    Text("This workout is no longer available.")
                    Button("Go back") { dismiss() }
                        .buttonStyle(.borderedProminent)
                }
                .padding()
            }
        }
        .background(FitTheme.background)
        .navigationTitle(template?.name ?? "Workout")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(handledCount > 0)
        .toolbar {
            if handledCount > 0 {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Back") { showExitConfirm = true }
                }
            }
        }
        .onAppear(perform: prepare)
        .task(id: isRunning) {
            // Simple one-second ticker that stops when paused or when the view goes away.
            while isRunning && !Task.isCancelled {
                try? await Task.sleep(for: .seconds(1))
                guard isRunning, !Task.isCancelled else { break }
                elapsedSeconds += 1
                if let timer = exerciseTimer, timer > 0 {
                    exerciseTimer = timer - 1
                }
            }
        }
        .confirmationDialog(
            "Leave this workout?",
            isPresented: $showExitConfirm,
            titleVisibility: .visible
        ) {
            Button("Leave", role: .destructive) { dismiss() }
            Button("Stay", role: .cancel) {}
        } message: {
            Text("Your progress in this session will not be saved unless you finish it.")
        }
    }

    private func content(_ template: WorkoutTemplate) -> some View {
        ScrollView {
            VStack(spacing: FitTheme.sectionSpacing) {
                timerCard(template)
                if let exercise = template.exercises.indices.contains(currentIndex)
                    ? template.exercises[currentIndex]
                    : nil {
                    currentExerciseCard(exercise, template: template)
                }
                allExercisesCard(template)

                Button {
                    finish(template)
                } label: {
                    Text(allHandled ? "Finish workout" : "Finish early & save")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(handledCount == 0)
                .padding(.top, 4)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 32)
        }
    }

    private func timerCard(_ template: WorkoutTemplate) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Elapsed").font(.caption).foregroundStyle(.secondary)
                    Text(Formatters.duration(elapsedSeconds))
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                }
                Spacer()
                Button {
                    isRunning.toggle()
                } label: {
                    Image(systemName: isRunning ? "pause.fill" : "play.fill")
                        .font(.title3)
                        .frame(width: 52, height: 52)
                        .background(FitTheme.brand)
                        .foregroundStyle(.white)
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(isRunning ? "Pause" : "Resume")
            }
            LabeledProgressBar(
                label: "Progress",
                valueText: "\(handledCount) / \(template.exercises.count)",
                fraction: template.exercises.isEmpty
                    ? 0
                    : Double(handledCount) / Double(template.exercises.count),
                color: FitTheme.brand
            )
            if !isRunning {
                Text("Paused").font(.caption.weight(.semibold)).foregroundStyle(.secondary)
            }
        }
        .fitCard(background: FitTheme.brand.opacity(0.12))
    }

    private func currentExerciseCard(_ exercise: Exercise, template: WorkoutTemplate) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Exercise \(currentIndex + 1) of \(template.exercises.count)")
                .font(.caption.weight(.bold))
                .foregroundStyle(FitTheme.workout)
            Text(exercise.name).font(.title3.bold())
            Text("\(exercise.sets) sets × \(exercise.repsLabel) · rest \(exercise.restSeconds)s")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(exercise.instructions).font(.subheadline)

            if exercise.isTimed {
                VStack(spacing: 6) {
                    Text(Formatters.duration(exerciseTimer ?? 0))
                        .font(.system(size: 40, weight: .bold, design: .rounded))
                    Text((exerciseTimer ?? 0) == 0 ? "Time's up" : "remaining in this set")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Button {
                        exerciseTimer = exercise.durationSeconds
                    } label: {
                        Label("Reset timer", systemImage: "arrow.counterclockwise")
                            .font(.caption)
                    }
                    .buttonStyle(.bordered)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
            }

            HStack(spacing: 10) {
                Button {
                    mark(.done, template: template)
                } label: {
                    Label("Done", systemImage: "checkmark").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                Button {
                    mark(.skipped, template: template)
                } label: {
                    Label("Skip", systemImage: "forward.end").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
        .fitCard()
    }

    private func allExercisesCard(_ template: WorkoutTemplate) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("All exercises").font(.headline)
            ForEach(Array(template.exercises.enumerated()), id: \.offset) { index, exercise in
                HStack(spacing: 10) {
                    Image(systemName: icon(for: statuses.indices.contains(index) ? statuses[index] : .pending))
                        .foregroundStyle(color(for: statuses.indices.contains(index) ? statuses[index] : .pending))
                        .frame(width: 22)
                    VStack(alignment: .leading, spacing: 1) {
                        Text(exercise.name)
                            .font(.subheadline)
                            .fontWeight(index == currentIndex ? .bold : .regular)
                        Text("\(exercise.sets) × \(exercise.repsLabel)")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 4)
                    if index != currentIndex,
                       statuses.indices.contains(index),
                       statuses[index] == .pending {
                        Button("Go") {
                            currentIndex = index
                            exerciseTimer = template.exercises[index].durationSeconds
                        }
                        .font(.caption)
                    }
                }
                .padding(.vertical, 3)
            }
        }
        .fitCard()
    }

    // MARK: - Logic

    private func prepare() {
        guard let template, statuses.isEmpty else { return }
        statuses = Array(repeating: .pending, count: template.exercises.count)
        exerciseTimer = template.exercises.first?.durationSeconds
        startedAt = Date()
        isRunning = true
    }

    private func mark(_ status: ExerciseStatus, template: WorkoutTemplate) {
        guard statuses.indices.contains(currentIndex) else { return }
        statuses[currentIndex] = status
        if let next = statuses.firstIndex(of: .pending) {
            currentIndex = next
        }
        exerciseTimer = template.exercises.indices.contains(currentIndex)
            ? template.exercises[currentIndex].durationSeconds
            : nil
    }

    private func finish(_ template: WorkoutTemplate) {
        guard handledCount > 0 else {
            store.show("Complete at least one exercise before finishing.")
            return
        }
        isRunning = false
        store.saveWorkout(
            template: template,
            startedAt: startedAt,
            finishedAt: Date(),
            durationSeconds: elapsedSeconds,
            completed: completedCount,
            skipped: skippedCount
        )
        dismiss()
    }

    private func icon(for status: ExerciseStatus) -> String {
        switch status {
        case .done: return "checkmark.circle.fill"
        case .skipped: return "minus.circle.fill"
        case .pending: return "circle"
        }
    }

    private func color(for status: ExerciseStatus) -> Color {
        switch status {
        case .done: return FitTheme.brand
        case .skipped: return .red
        case .pending: return .secondary
        }
    }
}
