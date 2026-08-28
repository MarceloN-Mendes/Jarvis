const imagens = {
    1: "img/gatinho1.jpg",
    2: "img/gatinho2.jpg",
    3: "img/gatinho3.jpg",
    4: "img/gatinho4.jpg",
    5: "img/gatinho5.jpg",
    6: "img/gatinho6.jpg",
    7: "img/gatinho7.jpg",
    8: "img/cavalo.jpg",
    9: "img/cavalo2.jpg",
    10: "img/cavalo3.jpg",
    11: "img/cavalo4.jpg",
    12: "img/cavalo5.jpg",
    14: "img/kkami.jpeg",
    15: "img/kkami2.jpeg",
    16: "img/kkami3.jpeg",
    17: "img/kkami4.jpeg",
    18: "img/kkami5.jpeg",
    19: "img/kkami6.jpeg",
    20: "img/kkami7.jpeg",
    21: "img/stella.jpeg",
    22: "img/stella2.jpeg",
};

const cantadas = {
    1: "Passei aqui só pra dizer que meu coração pediu seu contato.",
    2: "Se beleza desse dinheiro, você já tinha quebrado a economia.",
    3: "Nem te conheço direito e já tô aceitando convite pro casamento.",
    4: "Achei que meu dia já tava bom, aí você apareceu.",
    5: "Meu psicólogo disse pra correr atrás da felicidade. Oi.",
    6: "Desculpa a demora pra falar com você, tava ensaiando na frente do espelho.",
    7: "Não prometo o mundo, mas divido a última batata frita.",
    8: "Meu plano era só te olhar... aí inventei de me apaixonar.",
    9: "Se eu ganhar um sorriso seu, já considero meu dia vencido.",
    10: "Até o corretor do celular insiste em escrever seu nome.",
    11: "Você entrou na minha cabeça igual música chiclete.",
    12: "Não sou fotógrafo, mas já imaginei a gente juntos.",
    13: "Meu coração tá fazendo hora extra desde que te conheceu.",
    14: "Tô aceitando currículo pra vaga de amor da sua vida.",
    15: "Não tenho flores, mas trouxe coragem pra falar com você.",
    16: "Meu único talento foi te encontrar por acaso.",
    17: "Já que o destino fez a parte dele, vim fazer a minha.",
    18: "Fiquei tentando pensar numa cantada boa... mas aí você apareceu e eu esqueci tudo.",
    19: "A fila de quem quer seu coração tá grande, mas eu trouxe senha preferencial.",
    20: "Entre todas as coincidências do dia, você foi a minha favorita.",
    21: "Você não é Wi-Fi, mas sinto uma conexão muito forte entre nós.",
    22: "Você acredita em amor à primeira vista ou eu passo na sua frente de novo?",
    23: "Seu pai é padeiro? Porque você é um sonho.",
    24: "Você é câmera? Porque toda vez que te vejo eu sorrio.",
    25: "Você é estacionamento? Porque tem escrito 'você é meu' em todo lugar que eu olho.",
    26: "Você é Google? Porque tem tudo o que eu procuro.",
    27: "Seu nome é Netflix? Porque eu passaria horas com você.",
    28: "Você é boleto? Porque você venceu meu coração.",
    29: "Você é CPU? Porque sem você meu coração nem processa.",
    30: "Você é café? Porque tirou meu sono.",
    31: "Você é chuva? Porque acabou com meu penteado e minha estabilidade emocional.",
    32: "Você é teclado? Porque é exatamente meu tipo.",
    33: "Você é a prova de matemática? Porque eu não consigo tirar você da cabeça.",
    34: "Você é eclipse? Porque todo mundo para pra te admirar.",
    35: "Você é GPS? Porque desde que te encontrei nunca mais me perdi... só emocionalmente.",
    36: "Você é meu carregador? Porque sem você eu fico sem energia.",
    37: "Você é meu CPF? Porque eu queria ter você pra vida toda.",
    38: "Você é um PIX? Porque caiu direitinho no meu coração.",
    39: "Você é meu fone esquerdo? Porque quando some, minha vida perde o sentido.",
    40: "Você não é segunda-feira, mas conseguiu acabar comigo.",
    41: "Das ruinas que ainda vou viver, você foi a melhor das princesas que meus castelos tiveram da dadiva de proteger",
    42: "Conseguir ver você em cada detalhe do meu mundo me faz sentir que é nosso",
    43: "Sou TÃO SAGAZ, que consegui fazer você gostar de mim",
    44: "Apesar de tudo, entender você me faz gostar ainda mais de poder estar contigo",
    45: "Eu prometo não esquecer de você, que as músicas de amor sejam pra sempre",
    46: "cadê eu fiz um site não mereço nem um beijo"
};

// Chance (0 a 1) do botão da imagem especial aparecer a cada sorteio
const CHANCE_BOTAO_ESPECIAL = 0.1;

function sortear(dicionario) {
    const chaves = Object.keys(dicionario);
    const chaveSorteada = chaves[Math.floor(Math.random() * chaves.length)];
    return dicionario[chaveSorteada];
}

function sortearImagensSemRepetir(qtd, chavesPossiveis) {
    const embaralhadas = [...chavesPossiveis].sort(() => Math.random() - 0.5);
    return embaralhadas.slice(0, qtd).map((chave) => imagens[chave]);
}

function preencherImagens() {
    // Só as fotos de gatinho entram no grid normal (as de cavalo ficam reservadas pro especial)
    const chavesGatinho = Object.keys(imagens).filter((k) => !imagens[k].includes("eu"));
    const sorteadas = sortearImagensSemRepetir(6, chavesGatinho);

    document.getElementById("img1").src = sorteadas[0];
    document.getElementById("img2").src = sorteadas[1];
    document.getElementById("img3").src = sorteadas[2];
    document.getElementById("img4").src = sorteadas[3];
    document.getElementById("img5").src = sorteadas[4];
    document.getElementById("img6").src = sorteadas[5];
}

function preencherCantada() {
    document.getElementById("cantada").textContent = sortear(cantadas);
}

/**
 * Às vezes (CHANCE_BOTAO_ESPECIAL) mostra o botão "Clique". Se aparecer e o
 * usuário clicar, revela uma imagem especial (sorteada entre as de cavalo).
 */
function sortearBotaoEspecial() {
    const botao = document.getElementById("btnEspecial");
    const imgEspecial = document.getElementById("imgEspecial");

    // Reseta o estado a cada novo sorteio
    imgEspecial.classList.remove("visivel");
    imgEspecial.classList.add("escondida");

    const vaiAparecer = Math.random() < CHANCE_BOTAO_ESPECIAL;
    botao.classList.toggle("escondida", !vaiAparecer);
}

function revelarImagemEspecial() {
    const chavesCavalo = Object.keys(imagens).filter((k) => imagens[k].includes("cavalo"));
    const chaveSorteada = chavesCavalo[Math.floor(Math.random() * chavesCavalo.length)];

    const imgEspecial = document.getElementById("imgEspecial");
    imgEspecial.src = imagens[chaveSorteada];
    imgEspecial.classList.remove("escondida");
    // pequeno delay pra garantir que o navegador aplique a transição de opacidade
    requestAnimationFrame(() => imgEspecial.classList.add("visivel"));

    document.getElementById("btnEspecial").classList.add("escondida");
}

function fecharImgEspecial() {
    const imgEspecial = document.getElementById("imgEspecial");
    imgEspecial.classList.remove("visivel");
    imgEspecial.classList.add("escondida");

}

function sortearTudo() {
    preencherImagens();
    preencherCantada();
    sortearBotaoEspecial();
}

// -----------------------------------------------------------------------
// Integração com o reconhecimento de gesto (câmera / motor Python-Java)
// -----------------------------------------------------------------------
// Quando o back-end de gestos estiver rodando um servidor WebSocket, essa
// página escuta e recarrega sozinha ao receber um evento — sem precisar
// clicar no balão. Por enquanto, se não houver servidor rodando, o site
// funciona normalmente por clique (nada quebra).
const GESTURE_SOCKET_URL = "ws://localhost:8765";

function conectarGestureSocket() {
    try {
        const socket = new WebSocket(GESTURE_SOCKET_URL);

        socket.onmessage = (event) => {
            console.log("Gesto recebido da câmera:", event.data);
            sortearTudo();
        };

        socket.onerror = () => {
            console.log("Servidor de gestos não encontrado — funcionando só por clique, por enquanto.");
        };

        socket.onclose = () => {
            // tenta reconectar em alguns segundos, caso o servidor suba depois
            setTimeout(conectarGestureSocket, 5000);
        };
    } catch (erro) {
        console.log("WebSocket indisponível neste navegador:", erro);
    }
}

window.addEventListener("DOMContentLoaded", () => {
    sortearTudo();
    conectarGestureSocket();
});

document.getElementById("balao").addEventListener("click", sortearTudo);
document.getElementById("btnEspecial").addEventListener("click", revelarImagemEspecial);
document.getElementById("btnFechar").addEventListener("click", fecharImgEspecial);