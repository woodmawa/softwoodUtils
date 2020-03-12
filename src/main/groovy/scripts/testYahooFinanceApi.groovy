package scripts

import groovyx.net.http.AuthConfig
import groovyx.net.http.HTTPBuilder

//import java.net.http.HttpResponse

import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.HttpResponse
import groovy.json.JsonSlurper

import groovyx.net.http.HTTPBuilder


//really basic form - just use URL /UrlConnection
/*
def url = new URL('https://apidojo-yahoo-finance-v1.p.rapidapi.com/market/get-summary?region=US&lang=en')
URLConnection http = url.openConnection()

http.setRequestProperty('x-rapidapi-host', 'apidojo-yahoo-finance-v1.p.rapidapi.com')
http.setRequestProperty('x-rapidapi-key', '38797a919fmsh89b375ea99634cfp1991eajsn80a3a7cfb54d')


String result
InputStream is = http.getInputStream()
BufferedReader reader = new BufferedReader(new InputStreamReader(is))

result = reader.text

System.exit (1)
 */

//use Groovy HTTP Builder - this now works
HTTPBuilder request = new HTTPBuilder ('https://apidojo-yahoo-finance-v1.p.rapidapi.com')

request.setHeaders(['x-rapidapi-host': 'apidojo-yahoo-finance-v1.p.rapidapi.com',
                'x-rapidapi-key' : '38797a919fmsh89b375ea99634cfp1991eajsn80a3a7cfb54d'
])

request.get(path: '/market/get-summary',
        query: [region:'UK', lang:'en']
) { resp, json ->
        println resp.status
        println json
}

System.exit (1)

//this alternative uses Unirest client 
HttpResponse<String> response = Unirest.get("https://apidojo-yahoo-finance-v1.p.rapidapi.com/market/get-summary?region=UK&lang=en")
        .header("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com")
        .header("x-rapidapi-key", "38797a919fmsh89b375ea99634cfp1991eajsn80a3a7cfb54d")
        .asString();

JsonSlurper slurper = new JsonSlurper()
Map parsedJson = slurper.parseText (response.body)

parsedJson.'marketSummaryResponse'.'result'.each {
        println it
}


