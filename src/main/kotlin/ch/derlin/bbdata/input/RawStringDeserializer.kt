package ch.derlin.bbdata.input

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper


/**
 *
 * Problem: in the old api, people got used to send arrays or objects as "value",
 * while new version is expecting a string.
 *
 * This mapper always deserialize into strings.
 * The behavior is the same for values enclosed in quotes or basic values, it only changes for
 * raw arrays ("value": [...]) or objects (e.g. "value": {...}).
 *
 * Example:
 *  - "value": [1,   2,   false  ] => "[1,2,false]"
 *  - "value": {"a": 1, "b" : "x"} => "{\"a\":1,\"b\":\"x\"}",
 *  - "value": "[1,   2,false]" => "[1,   2,false]"
 *  - "value": 1 => "1.0"
 *
 * BEWARE: if not using quotes, the array or object MUST BE A CORRECT json array/object
 *
 * date: 30.09.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
class RawStringDeserializer : JsonDeserializer<String?>() {

    // basic objectMapper: no prettyPrint, nothing...
    private val objectMapper = ObjectMapper()

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): String? {
        // use default objectMapper for deserialization, so we keep the usual settings
        val node: JsonNode = (if (p.codec is ObjectMapper) p.codec else objectMapper).readTree(p)
        // Do not use the default objectMapper for serialization
        // since it may be configured with prettyPrint = true
        val ret = if (node.isTextual) node.asText() else objectMapper.writeValueAsString(node)
        return ret
    }
}