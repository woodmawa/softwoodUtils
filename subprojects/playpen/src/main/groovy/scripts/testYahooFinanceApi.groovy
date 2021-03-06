package scripts

import groovyx.net.http.AuthConfig
import groovyx.net.http.HTTPBuilder

//import java.net.http.HttpResponse

import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.HttpResponse
import groovy.json.JsonSlurper

import groovyx.net.http.HTTPBuilder

String apikey = System.getenv('RAPIDAPI-KEY')


/**
 * see https://rapidapi.com/blog/how-to-use-the-yahoo-finance-api/
 *
 * also
 * https://docs.rapidapi.com/docs/keys
 *
 * https://rapidapi.com/integraatio/api/morningstar1
 */

//really basic form - just use URL /UrlConnection
/*
def url = new URL('https://apidojo-yahoo-finance-v1.p.rapidapi.com/market/get-summary?region=US&lang=en')
URLConnection http = url.openConnection()

http.setRequestProperty('x-rapidapi-host', 'apidojo-yahoo-finance-v1.p.rapidapi.com')
http.setRequestProperty('x-rapidapi-key', apikey)


String result
InputStream is = http.getInputStream()
BufferedReader reader = new BufferedReader(new InputStreamReader(is))

result = reader.text

System.exit (1)
 */


//use Groovy HTTP Builder - this now works
HTTPBuilder request = new HTTPBuilder('https://apidojo-yahoo-finance-v1.p.rapidapi.com')

request.setHeaders(['x-rapidapi-host': 'apidojo-yahoo-finance-v1.p.rapidapi.com',
                    'x-rapidapi-key' : apikey
])

request.get(path: '/market/get-summary',
        query: [region: 'UK', lang: 'en']
) { resp, json ->
    println resp.status
    println json
}

System.exit(1)

//this alternative uses Unirest client
HttpResponse<String> response = Unirest.get("https://apidojo-yahoo-finance-v1.p.rapidapi.com/market/get-summary?region=UK&lang=en")
        .header("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com")
        .header("x-rapidapi-key", apikey)
        .asString();

JsonSlurper slurper = new JsonSlurper()
Map parsedJson = slurper.parseText(response.body)

parsedJson.'marketSummaryResponse'.'result'.each {
    println it
}


