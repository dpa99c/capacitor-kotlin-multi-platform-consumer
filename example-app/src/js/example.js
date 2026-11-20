import { KmpConsumer } from 'capacitor-kotlin-multi-platform-consumer';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    KmpConsumer.echo({ value: inputValue })
}
