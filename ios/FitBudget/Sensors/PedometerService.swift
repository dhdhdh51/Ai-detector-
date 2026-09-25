import Foundation
import CoreMotion
import OSLog

/// Reads the step count from CoreMotion.
///
/// `CMPedometer` keeps its own history on the device, so FitBudget simply asks for "steps since
/// midnight" whenever it becomes active - no background service and no battery cost. Devices
/// without the sensor (or when the user declines motion access) fall back to manual entry, which
/// the Steps screen offers explicitly.
@MainActor
final class PedometerService {

    private static let logger = Logger(subsystem: "com.fitbudget.app", category: "Pedometer")

    private let pedometer = CMPedometer()

    var isAvailable: Bool { CMPedometer.isStepCountingAvailable() }

    var authorizationStatus: CMAuthorizationStatus { CMPedometer.authorizationStatus() }

    var isAuthorized: Bool { authorizationStatus == .authorized }

    var isDenied: Bool {
        authorizationStatus == .denied || authorizationStatus == .restricted
    }

    /// CoreMotion has no explicit permission call: the system prompt appears on the first query,
    /// so we make a tiny one and discard the result.
    func requestAuthorization() async {
        guard isAvailable else { return }
        let start = Calendar.current.startOfDay(for: Date())
        _ = await stepCount(from: start, to: Date())
    }

    /// Steps recorded between two dates, or nil when unavailable/denied.
    func stepCount(from start: Date, to end: Date) async -> Int? {
        guard isAvailable, end > start else { return nil }

        return await withCheckedContinuation { continuation in
            var resumed = false
            pedometer.queryPedometerData(from: start, to: end) { data, error in
                // The handler is documented to run once, but guard anyway: resuming a
                // continuation twice would crash.
                guard !resumed else { return }
                resumed = true

                if let error {
                    Self.logger.info(
                        "Pedometer query failed: \(error.localizedDescription, privacy: .public)"
                    )
                    continuation.resume(returning: nil)
                    return
                }
                continuation.resume(returning: data?.numberOfSteps.intValue)
            }
        }
    }
}
