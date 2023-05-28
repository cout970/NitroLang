struct Box<#A> {
    value: #A
}

struct List<#A> {
    items: Box<#A>
    size: Int
    capacity: Int
}

struct Player {
    x: Int
    y: Int
    name: String
    health: Float
    max_health: Float
}

struct Entity {
    x: Int
    y: Int
    kind: String
}

struct World {
    player: Player
    entities: List<Entity>
    pending_save: Boolean
}


fun main(): Unit {
    let world = World $[
        player: Player $[
            x: 0
            y: 0
            name: "Anonymous"
            health: 100.0
            max_health: 100.0
        ]
        entities: #[
            Entity $[x: 50, y: 50, kind: "target"]
            Entity $[x: 25, y: 50, kind: "enemy"]
            Entity $[x: 50, y: 25, kind: "enemy"]
        ]
        pending_save: true
    ]
}