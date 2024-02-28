// access_script_vars {
// The most basic of k6 scripts.
export default function(){
    console.log(`k6 tests ${__ENV.MY_SCRIPT_VAR}`)
}
// }
