
// Trace: The most fine-grained information only used in rare cases where you need the full visibility of what is going on
fun Log::trace(message: String) = get_logger().trace(message)

// Debug: Less granular than trace but still useful to log information that can help debug an application
fun Log::debug(message: String) = get_logger().debug(message)

// Info: Informational messages that help understand what is going on inside the application
fun Log::info(message: String) = get_logger().info(message)

// Warn: Unexpected events or problems that might affect the application, but are not serious by themselves
fun Log::warn(message: String) = get_logger().warn(message)

// Error: Other runtime errors or unexpected conditions that might cause the application to stop working properly
fun Log::error(message: String) = get_logger().error(message)

// Fatal: Severe errors that cause the application to abort
fun Log::fatal(message: String) = get_logger().fatal(message)
