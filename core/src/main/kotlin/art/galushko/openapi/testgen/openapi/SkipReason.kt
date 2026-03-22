package art.galushko.openapi.testgen.openapi

/**
 * Represents the reason for skipping a specific operation or condition in the execution process.
 */
public enum class SkipReason {
    /**
     * Indicates that a cycle was detected during the processing or traversal of the test generation
     * workflow. This typically occurs when a recursive dependency or reference is encountered,
     * causing an infinite loop.
     *
     * Used as a reason to skip further processing of a specific node or entity to prevent
     * indefinite execution.
     */
    CYCLE_DETECTED,

    /**
     * Indicates that the depth of a structure has exceeded the allowable limit during generation or processing.
     * This is used as a reason for skipping further processing of an element or branch.
     */
    DEPTH_EXCEEDED,
}
