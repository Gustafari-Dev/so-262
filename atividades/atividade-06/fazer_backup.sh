#!/bin/bash
# Script de automação de backup do Projeto_A
echo "Iniciando o processo de backup..."
mkdir -p ~/Projeto_A/backup_geral
cp -r ~/Projeto_A/documentos/* ~/Projeto_A/backup_geral/
echo "Backup concluído com sucesso em: $(date)"
