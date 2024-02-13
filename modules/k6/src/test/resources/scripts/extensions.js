// extensions_script {
import YAML from "k6/x/yaml";

export default function(){
    const obj = YAML.parse(`
root:
    key1: extended
`
    );
    console.log(`k6 tests ${obj.root.key1}`);
}
// }
