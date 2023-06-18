fun main() {
    // #0
    let a: Float = 0.1

    if true {
        // #5
        let a: Int = 5
        a

        // Shadowing is allowed
        // #9
        let a: String = ""
        a

        if true {
            a
            if false {
                // #18
                let a: Int = 42
                a
            }
            a
        }

        // #25
        let a: Boolean = true
        a
    }
    a
}