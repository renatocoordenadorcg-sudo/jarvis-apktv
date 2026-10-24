# JARVIS TV (go2rtc) — APK separado para Android TV

App só para **Android TV**, modo **go2rtc** (sem aba Painel). Conecta direto no go2rtc (ex.: `192.168.3.45:1984`): não precisa do servidor JARVIS. O app do celular (`apk-github`) não é afetado: este é outro app (`com.jarvis.tv`), os dois podem ficar instalados.

## Gerar o APK no GitHub
1. Crie um repositório **novo** e envie o conteúdo desta pasta (arraste a pasta; se subirem soltos, o workflow se organiza sozinho).
2. **Actions** › *Build APK TV* (roda sozinho, ~4–7 min) › **Artifacts › JARVIS-TV-apk**: `JARVIS-TV.apk` (otimizado, **use este**) e `JARVIS-TV-debug.apk` (reserva).
3. Para baixar direto na TV: crie uma **Release** com tag `v1.0.0` e use, no app **Downloader**,
   `https://github.com/SEU_USUARIO/SEU_REPO/releases/latest/download/JARVIS-TV.apk`

## Primeiro uso
Abra o app › digite o endereço do go2rtc (`192.168.3.45:1984`; sem porta usa 1984). Menu do controle: Recarregar · Alterar servidor · **Abrir ao iniciar** (Grade ou Ronda) · Sair.

## Telas
- **Grade**: todas as câmeras (streams do go2rtc). Se existir `nome` e `nome_sub` (também `-sub`, `_sd`), o `_sub` vira o **SD** da câmera `nome` (HD). Botões: **Grade** Imagens/Vídeo · **Colunas** Auto/2–6 · **Imagem** Auto/Preencher/Esticar · **Ronda** · **Atualizar**.
- **Câmera em tela cheia** (OK numa câmera): ◄ ► troca de câmera · ▲ HD · ▼ SD · OK/Voltar fecha.
- **Ronda**: escolha as câmeras (caixas de marcar), monte as etapas (N câmeras × segundos; ex. 1 → 4 → 6) e inicie. Em várias câmeras usa o **SD** (mais leve); numa só usa o **HD**. ◄ ► anterior/próxima · OK pausa · Voltar sai · Play/Pause e CH+/CH− do controle também funcionam. A escolha fica salva na TV.
- **Imagem** (grade, câmera em tela cheia e ronda):
  - *Auto*: na **grade**, cada quadrado assume a proporção da própria câmera (sem tarja e sem corte). Na **ronda** e na **tela cheia**, a tela é toda ocupada e a imagem preenche o quadrado (corta só o excedente); o app escolhe linhas/colunas que se pareçam com as câmeras para cortar o mínimo. Só mostra a imagem inteira, com faixas, se a câmera for muito diferente do quadrado (mais de ~2,2×, ex.: câmera horizontal numa tela em pé).
  - *Preencher*: sempre cobre o quadrado, cortando as bordas.
  - *Esticar*: sempre cobre o quadrado mostrando a imagem inteira (distorce se a proporção for diferente).
  - Dica: se o corte incomodar, use menos câmeras por etapa na ronda (ex.: 4 ou 6 em vez de 9) ou o modo *Esticar*.

## Como funciona (e limites)
- A interface está dentro do app (`assets/tv.html`) e é apresentada no endereço do go2rtc, então fala com ele na mesma origem (sem CORS, sem mexer na configuração do go2rtc).
- A Grade em **Imagens** usa fotos (`/api/frame.jpeg`) atualizadas só nas câmeras visíveis — é o modo mais leve. **Vídeo** abre um vídeo por câmera (pesado em TV Box simples).
- Câmeras H.265: o app nunca pede H.265 ao go2rtc; se o stream do go2rtc só tiver H.265 e não houver conversão, a imagem não aparece (adicione `ffmpeg:NOME#video=h264` como 2ª fonte do stream ou mude a câmera para H.264).
- TV ligada 24 h: reconecta sozinha, recria a tela se o sistema matar o WebView e recarrega a cada 6 h.
