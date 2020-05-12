package playpen.modelling

trait ModelBaseTrait {
    String baseType = "<undefined>"
    String schemaLocation = 'uri../'
    String schemaBase = "com/softwood"
    String server = 'https://localhost'
    String port = '8080'
    String id
    String name

    String getUrlName () {
        "$server:$port/$schemaBase/$schemaLocation"
    }

    String getType () {
        this.getClass().simpleName
    }

    String toString() {
        "baseType:@$baseType, type:@${this.getClass().simpleName}, schemaLocation:@${schemaLocation}"
    }

}