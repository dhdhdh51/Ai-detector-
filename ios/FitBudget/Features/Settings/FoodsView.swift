import SwiftUI
import FitBudgetCore

struct FoodsView: View {

    @Environment(AppStore.self) private var store

    @State private var query = ""
    @State private var editorFood: FoodRecord?
    @State private var showNewFood = false
    @State private var pendingDelete: FoodRecord?
    @State private var showRestoreConfirm = false

    private var results: [FoodRecord] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !trimmed.isEmpty else { return store.foods }
        return store.foods.filter { $0.nameKey.contains(trimmed) }
    }

    var body: some View {
        List {
            Section {
                Text(
                    "\(store.foods.count) foods · \(store.foods.filter(\.isCustom).count) added by you · "
                    + "\(store.settings.excludedFoodKeys.count) excluded from suggestions"
                )
                .font(.caption)
                .foregroundStyle(.secondary)
            }

            ForEach(results) { food in
                foodRow(food)
            }

            Section {
                Text(
                    "Calorie, protein and cost values are approximate reference figures — edit any "
                    + "food to match what you actually eat and pay."
                )
                .font(.caption2)
                .foregroundStyle(.secondary)
            }
        }
        .searchable(text: $query, prompt: "Search foods")
        .navigationTitle("Food database")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showNewFood = true
                } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel("Add food")
            }
            ToolbarItem(placement: .bottomBar) {
                Button("Restore built-in list") { showRestoreConfirm = true }
                    .font(.footnote)
            }
        }
        .sheet(isPresented: $showNewFood) {
            FoodEditorSheet(food: nil)
        }
        .sheet(item: $editorFood) { food in
            FoodEditorSheet(food: food)
        }
        .confirmationDialog(
            pendingDelete.map { "Delete \($0.name)?" } ?? "Delete food?",
            isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let pendingDelete { store.delete(pendingDelete) }
                pendingDelete = nil
            }
            Button("Cancel", role: .cancel) { pendingDelete = nil }
        } message: {
            Text(
                pendingDelete?.isCustom == true
                    ? "This food will be removed from your database. Meals already logged keep their values."
                    : "This is a built-in food. You can restore the built-in list later from this screen."
            )
        }
        .confirmationDialog(
            "Restore built-in foods?",
            isPresented: $showRestoreConfirm,
            titleVisibility: .visible
        ) {
            Button("Restore", role: .destructive) { store.restoreSeedFoods() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("The bundled Indian food list will be reinstalled. Foods you added yourself will be removed.")
        }
    }

    private func foodRow(_ food: FoodRecord) -> some View {
        let excluded = store.settings.excludedFoodKeys.contains(food.nameKey)
        return VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 4) {
                Text(food.name).font(.subheadline.weight(.semibold))
                if food.isCustom {
                    Text("· yours").font(.caption2).foregroundStyle(FitTheme.brand)
                }
            }
            Text(
                "\(food.servingLabel) · \(Int(food.calories.rounded())) kcal · "
                + "\(Formatters.grams(food.proteinG)) protein · \(Formatters.rupees(food.costRupees))"
            )
            .font(.caption2)
            .foregroundStyle(.secondary)
            Text(
                "\(food.category.label) · \(food.role.label) · "
                + String(format: "%.2f g protein per ₹", food.proteinPerRupee)
            )
            .font(.caption2)
            .foregroundStyle(.secondary)
            if excluded {
                Text("Excluded from suggestions").font(.caption2).foregroundStyle(.red)
            }
        }
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) { pendingDelete = food } label: {
                Label("Delete", systemImage: "trash")
            }
            Button { editorFood = food } label: {
                Label("Edit", systemImage: "pencil")
            }
            .tint(.blue)
        }
        .swipeActions(edge: .leading) {
            Button {
                store.toggleExcludedFood(food.nameKey)
            } label: {
                Label(excluded ? "Allow" : "Exclude", systemImage: excluded ? "checkmark" : "nosign")
            }
            .tint(excluded ? .green : .orange)
        }
    }
}

struct FoodEditorSheet: View {

    let food: FoodRecord?

    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var servingLabel = "1 serving"
    @State private var calories = ""
    @State private var protein = ""
    @State private var cost = ""
    @State private var category: FoodCategory = .veg
    @State private var role: FoodRole = .protein
    @State private var error: String?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Name", text: $name)
                    TextField("Serving, e.g. 1 bowl (150 g)", text: $servingLabel)
                }
                Section("Per serving") {
                    HStack {
                        Text("Calories")
                        Spacer()
                        TextField("kcal", text: $calories)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                    HStack {
                        Text("Protein")
                        Spacer()
                        TextField("g", text: $protein)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                    }
                    HStack {
                        Text("Cost")
                        Spacer()
                        TextField("₹", text: $cost)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                    }
                }
                Section {
                    Picker("Category", selection: $category) {
                        ForEach(FoodCategory.allCases, id: \.self) { Text($0.label).tag($0) }
                    }
                    Picker("Role in a meal", selection: $role) {
                        ForEach(FoodRole.allCases, id: \.self) { Text($0.label).tag($0) }
                    }
                }
                if let error {
                    Section { Text(error).font(.caption).foregroundStyle(.red) }
                }
            }
            .navigationTitle(food == nil ? "Add a food" : "Edit food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") { save() }
                }
            }
            .task { load() }
        }
    }

    private func load() {
        guard let food else { return }
        name = food.name
        servingLabel = food.servingLabel
        calories = "\(Int(food.calories))"
        protein = String(format: "%.1f", food.proteinG)
        cost = "\(Int(food.costRupees))"
        category = food.category
        role = food.role
    }

    private func save() {
        let caloriesValue = Validators.parseDecimal(calories)
        let proteinValue = Validators.parseDecimal(protein)
        let costValue = Validators.parseDecimal(cost)

        for result in [
            Validators.foodName(name),
            Validators.servingLabel(servingLabel),
            Validators.calories(caloriesValue),
            Validators.protein(proteinValue),
            Validators.cost(costValue)
        ] where !result.isValid {
            error = result.message
            return
        }

        guard let caloriesValue, let proteinValue, let costValue else { return }

        let outcome: Result<Void, StoreError>
        if let food {
            outcome = store.updateFood(
                food,
                name: name,
                servingLabel: servingLabel,
                calories: caloriesValue,
                proteinG: proteinValue,
                costRupees: costValue,
                category: category,
                role: role
            )
        } else {
            outcome = store.addCustomFood(
                name: name,
                servingLabel: servingLabel,
                calories: caloriesValue,
                proteinG: proteinValue,
                costRupees: costValue,
                category: category,
                role: role
            )
        }

        switch outcome {
        case .success:
            store.show(food == nil ? "\(name) added to your food database." : "\(name) updated.")
            dismiss()
        case .failure(let failure):
            error = failure.errorDescription
        }
    }
}
