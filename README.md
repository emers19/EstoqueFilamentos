# Estoque de Filamentos - Android

Aplicativo Android offline para gestão de rolos de filamento **PLA, PETG e TPU**.

## Recursos da versão 1.1
- Categorias PLA, PETG e TPU
- Cadastro de cor e marca/fabricante
- Peso inicial e peso restante
- Validação para impedir pesos inválidos
- Observações e especificações/conteúdo do QR Code
- Foto do rolo pela câmera ou galeria
- Foto específica do QR Code
- Pesquisa por cor, marca, observação ou especificação
- Painel de quantidade por material
- Aviso visual quando o peso restante chega a 20% ou menos
- Edição e exclusão de cadastros
- Banco SQLite local/offline
- Captura de câmera via FileProvider, compatível sem permissão de armazenamento
- GitHub Actions pronto para gerar um APK instalável

## Compatibilidade
- Android mínimo: 8.0 (API 26)
- Target/Compile SDK: 35
- Java: 17
- Android Gradle Plugin: 8.7.3
- Gradle usado no GitHub Actions: 8.9

## Gerar o APK no GitHub

O arquivo `.github/workflows/build-apk.yml` já está configurado.

1. Crie um repositório no GitHub.
2. Envie **o conteúdo desta pasta** para a raiz do repositório. A raiz deve conter `settings.gradle`, `build.gradle`, `app/` e `.github/`.
3. Abra a aba **Actions** do repositório.
4. Abra o workflow **Gerar APK Android**.
5. Clique em **Run workflow** e confirme em **Run workflow**.
6. Aguarde a execução ficar verde.
7. Abra a execução concluída.
8. Na seção **Artifacts**, baixe **EstoqueFilamentos-APK**.
9. Extraia o ZIP baixado pelo GitHub. Dentro estará `EstoqueFilamentos.apk`.
10. Transfira o APK para o Android e permita a instalação de apps desconhecidos quando o sistema solicitar.

O workflow também executa automaticamente após `push` nas branches `main` ou `master`.

## Observação sobre assinatura
O workflow atual gera um **APK de debug**, assinado automaticamente pelo Android e adequado para instalação e testes pessoais. Para distribuição pública/Play Store, configure posteriormente uma assinatura de release com um keystore protegido por GitHub Secrets.
