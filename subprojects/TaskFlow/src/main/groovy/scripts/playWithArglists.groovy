package scripts


/*  Oject [] will work, args by itself wont - treated as Object) */
def func (String name, Object[] args) {
    println "$name, with $args"
}

func ('method', '1', '2', '3')