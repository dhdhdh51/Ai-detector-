import SwiftUI
import FitBudgetCore

struct DietView: View {

    @Environment(AppStore.self) private var store

    @State private var pickerTarget: FoodPickerTarget?
    @State private var quantityItem: MealEntryRecord?
    @State private var customMealFood: FoodRecord?
    @State private var timePickerGroup: MealGroup?
    @State private var showClearConfirm = false

    private var day: DayKey { store.dietDay }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: FitTheme.sectionSpacing) {
                dayHeader
                totalsCard

                if store.dietGroups.isEmpty {
                    EmptyStateView(
                        systemImage: "fork.knife.circle",
                        title: "No meals planned",
                        message: "Generate an affordable plan from your food database, or add meals yourself.",
                        actionTitle: "Generate plan"
                    ) {
                        store.regeneratePlan(for: day)
                    }
                    .fitCard()
                }

                ForEach(store.dietGroups) { group in
                    mealSection(group)
                }

                HStack(spacing: 10) {
                    Button {
                        pickerTarget = .customMeal
                    } label: {
                        Label("Custom meal", systemImage: "plus")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    Button {
                        store.regeneratePlan(for: day)
                    } label: {
                        Label("New plan", systemImage: "arrow.triangle.2.circlepath")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }

                if !store.dietGroups.isEmpty {
                    Button("Clear this day's plan", role: .destructive) {
                        showClearConfirm = true
                    }
                    .font(.subheadline)
                }

                DisclaimerCard()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(FitTheme.background)
        .navigationTitle("Diet")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $pickerTarget) { target in
            FoodPickerSheet(title: target.title) { food in
                switch target {
                case .addTo(let group):
                    store.addFood(food, to: group)
                case .replace(let item):
                    store.replaceFood(item, with: food)
                case .customMeal:
                    customMealFood = food
                }
                pickerTarget = nil
            }
        }
        .sheet(item: $quantityItem) { item in
            QuantitySheet(item: item) { quantity in
                store.updateQuantity(item, quantity: quantity)
                quantityItem = nil
            }
        }
        .sheet(item: $customMealFood) { food in
            CustomMealSheet(food: food) { label, hour, minute, quantity in
                store.addCustomMeal(label: label, hour: hour, minute: minute, food: food, quantity: quantity)
                customMealFood = nil
            }
        }
        .sheet(item: $timePickerGroup) { group in
            MealTimeSheet(group: group) { hour, minute in
                store.updateMealTime(group, hour: hour, minute: minute)
                timePickerGroup = nil
            }
        }
        .confirmationDialog(
            "Clear this plan?",
            isPresented: $showClearConfirm,
            titleVisibility: .visible
        ) {
            Button("Clear", role: .destructive) { store.clearDay(day) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Every meal logged for \(Formatters.relativeDay(day, today: store.todayKey).lowercased()) will be removed.")
        }
    }

    // MARK: - Sections

    private var dayHeader: some View {
        HStack {
            Button {
                store.selectDietDay(day - 1)
            } label: {
                Image(systemName: "chevron.left")
            }
            .buttonStyle(.bordered)

            VStack(spacing: 2) {
                Text(Formatters.relativeDay(day, today: store.todayKey))
                    .font(.headline)
                Text(Formatters.fullDate(day))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if day != store.todayKey {
                    Button("Back to today") { store.selectDietDay(store.todayKey) }
                        .font(.caption)
                }
            }
            .frame(maxWidth: .infinity)

            Button {
                store.selectDietDay(day + 1)
            } label: {
                Image(systemName: "chevron.right")
            }
            .buttonStyle(.bordered)
        }
        .padding(.top, 4)
    }

    private var totalsCard: some View {
        let groups = store.dietGroups
        let totalCalories = groups.reduce(0) { $0 + $1.calories }
        let totalCost = groups.reduce(0) { $0 + $1.cost }
        let completed = groups.flatMap(\.items).filter(\.completed)
        let eatenCalories = completed.reduce(0) { $0 + $1.totalCalories }
        let eatenProtein = completed.reduce(0) { $0 + $1.totalProtein }
        let eatenCost = completed.reduce(0) { $0 + $1.totalCost }
        let calorieTarget = store.profile?.estimatedCalorieTarget ?? 0
        let proteinTarget = store.profile?.proteinTargetGrams ?? 0
        let budget = store.profile?.dailyBudget ?? 0
        let overBudget = budget > 0 && totalCost > budget

        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "fork.knife").foregroundStyle(FitTheme.diet)
                Text("Planned today").font(.headline)
                Spacer()
                Text("\(Int(totalCalories.rounded())) kcal").font(.headline)
            }
            LabeledProgressBar(
                label: "Calories eaten",
                valueText: "\(Int(eatenCalories.rounded())) / \(Int(calorieTarget.rounded())) kcal",
                fraction: calorieTarget > 0 ? eatenCalories / calorieTarget : 0,
                color: FitTheme.diet
            )
            LabeledProgressBar(
                label: "Protein eaten",
                valueText: "\(Int(eatenProtein.rounded())) / \(Int(proteinTarget.rounded())) g",
                fraction: proteinTarget > 0 ? eatenProtein / proteinTarget : 0,
                color: FitTheme.weight
            )
            LabeledProgressBar(
                label: overBudget ? "Planned cost is over budget" : "Planned cost",
                valueText: "\(Formatters.rupees(totalCost)) / \(Formatters.rupees(budget))",
                fraction: budget > 0 ? totalCost / budget : 0,
                color: overBudget ? .red : FitTheme.budget
            )
            Text("Spent so far (ticked off): \(Formatters.rupees(eatenCost))")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .fitCard()
    }

    private func mealSection(_ group: MealGroup) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                Button {
                    store.setMealCompleted(group, completed: !group.allCompleted)
                } label: {
                    Image(systemName: group.allCompleted ? "checkmark.square.fill" : "square")
                        .foregroundStyle(group.allCompleted ? FitTheme.brand : Color.secondary)
                }
                .buttonStyle(.plain)

                VStack(alignment: .leading, spacing: 2) {
                    Text(group.label).font(.headline)
                    Text(
                        "\(Formatters.time(minutesOfDay: group.minutesOfDay)) · "
                        + "\(Int(group.calories.rounded())) kcal · "
                        + "\(Formatters.grams(group.protein)) protein · "
                        + Formatters.rupees(group.cost)
                    )
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                }
                Spacer(minLength: 8)
                Button("Time") { timePickerGroup = group }
                    .font(.caption)
            }

            ForEach(group.items) { item in
                mealRow(item)
            }

            Button {
                pickerTarget = .addTo(group)
            } label: {
                Label("Add food", systemImage: "plus")
                    .font(.caption.weight(.semibold))
            }
            .padding(.top, 2)
        }
        .fitCard()
    }

    private func mealRow(_ item: MealEntryRecord) -> some View {
        HStack(spacing: 10) {
            Button {
                store.setItemCompleted(item, completed: !item.completed)
            } label: {
                Image(systemName: item.completed ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(item.completed ? FitTheme.brand : Color.secondary)
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(Formatters.quantity(item.quantity)) × \(item.foodName)")
                    .font(.subheadline)
                    .strikethrough(item.completed)
                Text(
                    "\(item.servingLabel) · \(Int(item.totalCalories.rounded())) kcal · "
                    + "\(Formatters.grams(item.totalProtein)) · \(Formatters.rupees(item.totalCost))"
                )
                .font(.caption2)
                .foregroundStyle(.secondary)
            }
            Spacer(minLength: 4)

            Menu {
                Button("Change quantity") { quantityItem = item }
                Button("Replace food") { pickerTarget = .replace(item) }
                Button("Never suggest this") {
                    store.toggleExcludedFood(item.foodNameKey)
                    store.show("\(item.foodName) will not be suggested again.")
                }
                Button("Remove", role: .destructive) { store.delete(item) }
            } label: {
                Image(systemName: "ellipsis.circle").foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 2)
    }
}

// MARK: - Sheets

enum FoodPickerTarget: Identifiable {
    case addTo(MealGroup)
    case replace(MealEntryRecord)
    case customMeal

    var id: String {
        switch self {
        case .addTo(let group): return "add-\(group.id)"
        case .replace(let item): return "replace-\(item.persistentModelID.hashValue)"
        case .customMeal: return "custom"
        }
    }

    var title: String {
        switch self {
        case .addTo(let group): return "Add to \(group.label)"
        case .replace(let item): return "Replace \(item.foodName)"
        case .customMeal: return "Pick a food for your meal"
        }
    }
}

struct FoodPickerSheet: View {

    let title: String
    let onSelect: (FoodRecord) -> Void

    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""

    private var results: [FoodRecord] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !trimmed.isEmpty else { return store.foods }
        return store.foods.filter { $0.nameKey.contains(trimmed) }
    }

    var body: some View {
        NavigationStack {
            List {
                if results.isEmpty {
                    Text("No foods match \"\(query)\". Add it in Profile → Food database.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(results) { food in
                        Button {
                            onSelect(food)
                            dismiss()
                        } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(food.name).font(.subheadline.weight(.semibold))
                                Text(
                                    "\(food.servingLabel) · \(Int(food.calories.rounded())) kcal · "
                                    + "\(Formatters.grams(food.proteinG)) protein · "
                                    + Formatters.rupees(food.costRupees)
                                )
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .searchable(text: $query, prompt: "Search foods")
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

struct QuantitySheet: View {

    let item: MealEntryRecord
    let onSave: (Double) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var quantity: Double

    init(item: MealEntryRecord, onSave: @escaping (Double) -> Void) {
        self.item = item
        self.onSave = onSave
        _quantity = State(initialValue: item.quantity)
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text(
                    "1 serving = \(item.servingLabel) · \(Int(item.caloriesPerServing.rounded())) kcal · "
                    + Formatters.rupees(item.costPerServing)
                )
                .font(.caption)
                .foregroundStyle(.secondary)

                QuantityStepper(quantity: $quantity)
                    .frame(maxWidth: .infinity, alignment: .center)

                Text(
                    "\(Int((item.caloriesPerServing * quantity).rounded())) kcal · "
                    + "\(Formatters.grams(item.proteinPerServing * quantity)) protein · "
                    + Formatters.rupees(item.costPerServing * quantity)
                )
                .font(.subheadline)

                Spacer()
            }
            .padding(20)
            .navigationTitle(item.foodName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") { onSave(quantity) }
                }
            }
        }
        .presentationDetents([.medium])
    }
}

struct CustomMealSheet: View {

    let food: FoodRecord
    let onAdd: (String, Int, Int, Double) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var label = "Extra meal"
    @State private var time = Date()
    @State private var quantity: Double = 1

    var body: some View {
        NavigationStack {
            Form {
                Section("Meal") {
                    TextField("Meal name", text: $label)
                    DatePicker("Meal time", selection: $time, displayedComponents: .hourAndMinute)
                }
                Section("Food") {
                    Text("\(food.name) · \(food.servingLabel)").font(.subheadline)
                    QuantityStepper(quantity: $quantity)
                }
            }
            .navigationTitle("New meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Add") {
                        let components = DayCalendar.calendar().dateComponents([.hour, .minute], from: time)
                        onAdd(label, components.hour ?? 16, components.minute ?? 0, quantity)
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
}

struct MealTimeSheet: View {

    let group: MealGroup
    let onSave: (Int, Int) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var time: Date

    init(group: MealGroup, onSave: @escaping (Int, Int) -> Void) {
        self.group = group
        self.onSave = onSave
        var components = DateComponents()
        components.hour = group.minutesOfDay / 60
        components.minute = group.minutesOfDay % 60
        _time = State(initialValue: DayCalendar.calendar().date(from: components) ?? Date())
    }

    var body: some View {
        NavigationStack {
            VStack {
                DatePicker("", selection: $time, displayedComponents: .hourAndMinute)
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                Spacer()
            }
            .padding()
            .navigationTitle("\(group.label) time")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Set") {
                        let components = DayCalendar.calendar().dateComponents([.hour, .minute], from: time)
                        onSave(components.hour ?? 8, components.minute ?? 0)
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
}
