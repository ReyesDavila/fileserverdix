#!/bin/bash

# ==========================================
# CONFIGURACIÓN DEL SISTEMA (BASE DE DATOS LOCAL)
# ==========================================
CONF_DIR="$HOME/.gitAuto2_data"
PASS_FILE="$CONF_DIR/master_pass.txt"
USERS_FILE="$CONF_DIR/users.txt"
REPOS_FILE="$CONF_DIR/repos.txt"
DIR_LOCAL_PATH="../versiones_locales/$(basename "$PWD")"

mkdir -p "$CONF_DIR"
touch "$USERS_FILE"
touch "$REPOS_FILE"

# Auto-ignorar este script en Git
if ! grep -q "$(basename "$0")" .gitignore 2>/dev/null; then
    echo -e "\n# Script de automatizacion local\n$(basename "$0")" >> .gitignore
fi

# ==========================================
# PALETA DE COLORES NEON-DARK
# ==========================================
MAGENTA='\033[1;35m'
CYAN='\033[1;36m'
VERDE='\033[1;32m'
AMARILLO='\033[1;33m'
BLANCO='\033[1;37m'
ROJO='\033[1;31m'
NC='\033[0m'

# ==========================================
# ARTE ASCII KAWAII
# ==========================================
mostrar_header() {
    clear
    echo -e "${CYAN}"
    echo " ⋆⁺₊⋆ ☾ ⋆⁺₊⋆ ☁︎  ⋆⁺₊⋆ ☾ ⋆⁺₊⋆ ☁︎  ⋆⁺₊⋆ ☾ ⋆⁺₊⋆ ☁︎ "
    echo " ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓"
    echo " ┃  ${MAGENTA}✧ G I T   A U T O   P R O   V 2.0 ✧${CYAN}  ┃"
    echo " ┃        ${BLANCO}(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧ Admin Edition${CYAN}        ┃"
    echo " ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛${NC}"
}

# ==========================================
# SEGURIDAD MAESTRA
# ==========================================
verificar_seguridad() {
    if [ ! -f "$PASS_FILE" ]; then
        echo -e "${AMARILLO}--- CONFIGURACIÓN INICIAL DEL SISTEMA ---${NC}"
        read -s -p "Crea tu Contraseña Maestra: " P1; echo ""
        read -s -p "Confirma tu Contraseña: " P2; echo ""
        if [ "$P1" == "$P2" ] && [ -n "$P1" ]; then
            echo "$P1" > "$PASS_FILE"
            echo -e "${VERDE}✔ Sistema protegido exitosamente.${NC}"
            sleep 1
        else
            echo -e "${ROJO}Error: Las contraseñas no coinciden.${NC}"; exit 1
        fi
    else
        read -s -p "🔑 Ingresa Contraseña Maestra: " ACCESO; echo ""
        if [ "$ACCESO" != "$(cat "$PASS_FILE")" ]; then
            echo -e "${ROJO}Acceso denegado. (ꐦ ಠ_ಠ)${NC}"; exit 1
        fi
    fi
}

pedir_pass() {
    read -s -p "🔑 Confirma Contraseña Maestra para autorizar: " INTENTO; echo ""
    if [ "$INTENTO" != "$(cat "$PASS_FILE")" ]; then echo -e "${ROJO}Denegado.${NC}"; return 1; else return 0; fi
}

# ==========================================
# MENÚS DE ADMINISTRACIÓN
# ==========================================
admin_usuarios() {
    while true; true; do
        mostrar_header
        echo -e "${MAGENTA}--- 👥 GESTIÓN DE USUARIOS ---${NC}"
        echo -e "${CYAN}1)${NC} Añadir Usuario"
        echo -e "${CYAN}2)${NC} Listar Usuarios"
        echo -e "${CYAN}3)${NC} Eliminar Usuario"
        echo -e "${CYAN}4)${NC} Volver"
        read -p "❯ Elige: " OPT
        case $OPT in
            1) 
               read -p "Nombre de usuario GitHub: " U_NAME
               read -p "Correo de GitHub: " U_EMAIL
               echo -n "Token de acceso (oculto): "; stty -echo; read U_TOKEN; stty echo; echo ""
               if ! grep -q "^$U_NAME:" "$USERS_FILE"; then
                   echo "$U_NAME:$U_EMAIL:$U_TOKEN" >> "$USERS_FILE"
                   echo -e "${VERDE}✔ Usuario $U_NAME añadido.${NC}"
               else
                   echo -e "${ROJO}El usuario ya existe.${NC}"
               fi
               sleep 2 ;;
            2) 
               echo -e "\n${AMARILLO}Usuarios Registrados:${NC}"
               awk -F':' '{print "- " $1 " (" $2 ")"}' "$USERS_FILE"
               read -p "Enter para continuar..." ;;
            3) 
               read -p "Nombre de usuario a eliminar: " U_DEL
               if pedir_pass; then
                   sed -i "/^$U_DEL:/d" "$USERS_FILE"
                   sed -i "/^$U_DEL:/d" "$REPOS_FILE" # Borra sus repos vinculados
                   echo -e "${VERDE}✔ Usuario eliminado del sistema local.${NC}"
               fi
               sleep 2 ;;
            4) break ;;
        esac
    done
}

admin_repos() {
    while true; true; do
        mostrar_header
        echo -e "${MAGENTA}--- 📁 GESTIÓN DE REPOSITORIOS ---${NC}"
        echo -e "${CYAN}1)${NC} Añadir Repositorio a un Usuario"
        echo -e "${CYAN}2)${NC} Listar Repositorios"
        echo -e "${CYAN}3)${NC} Quitar Repositorio de la lista local"
        echo -e "${CYAN}4)${NC} Volver"
        read -p "❯ Elige: " OPT
        case $OPT in
            1) 
               echo -e "\n${AMARILLO}Usuarios disponibles:${NC}"
               awk -F':' '{print $1}' "$USERS_FILE"
               read -p "Escribe el Usuario: " R_USER
               if grep -q "^$R_USER:" "$USERS_FILE"; then
                   read -p "Nombre del Repositorio (Exacto al de GitHub): " R_NAME
                   echo "$R_USER:$R_NAME" >> "$REPOS_FILE"
                   echo -e "${VERDE}✔ Repositorio vinculado exitosamente.${NC}"
               else
                   echo -e "${ROJO}Usuario no encontrado.${NC}"
               fi
               sleep 2 ;;
            2) 
               echo -e "\n${AMARILLO}Repositorios Vinculados [Usuario -> Repo]:${NC}"
               awk -F':' '{print "- " $1 " ➔  " $2}' "$REPOS_FILE"
               read -p "Enter para continuar..." ;;
            3) 
               read -p "Nombre del Repositorio a quitar de la lista: " R_DEL
               if pedir_pass; then
                   sed -i "/:$R_DEL$/d" "$REPOS_FILE"
                   echo -e "${VERDE}✔ Borrado de la lista local (Sigue a salvo en GitHub).${NC}"
               fi
               sleep 2 ;;
            4) break ;;
        esac
    done
}

respaldo_local() {
    mkdir -p "$DIR_LOCAL_PATH"
    NUM=$(ls -1 "$DIR_LOCAL_PATH" 2>/dev/null | grep -E '^ver[0-9]+$' | wc -l)
    VER_NAME=$(printf "ver%02d" $((NUM + 1)))
    DESTINO="$DIR_LOCAL_PATH/$VER_NAME"
    mkdir -p "$DESTINO"
    rsync -a --exclude='.git' ./* "$DESTINO/" 2>/dev/null
    echo -e "${VERDE}✔ Respaldo físico guardado en: $DESTINO${NC}"
    sleep 2
}

# ==========================================
# TRABAJO EN GITFLOW (EL NÚCLEO)
# ==========================================
trabajar_git() {
    mostrar_header
    echo -e "${AMARILLO}Selecciona el repositorio activo para esta carpeta:${NC}"
    nl -s ") " "$REPOS_FILE" | awk -F':' '{print $1 " ➔ " $2}'
    read -p "Número: " SEL
    if [ -z "$SEL" ]; then return; fi
    
    LINEA_REPO=$(sed -n "${SEL}p" "$REPOS_FILE")
    W_USER=$(echo "$LINEA_REPO" | cut -d':' -f1)
    W_REPO=$(echo "$LINEA_REPO" | cut -d':' -f2)
    
    LINEA_USER=$(grep "^$W_USER:" "$USERS_FILE")
    W_EMAIL=$(echo "$LINEA_USER" | cut -d':' -f2)
    W_TOKEN=$(echo "$LINEA_USER" | cut -d':' -f3)

    # Inicializar y configurar
    git config --global user.name "$W_USER"
    git config --global user.email "$W_EMAIL"
    if [ ! -d ".git" ]; then
        git init -q
        git branch -M main
        echo -e "${VERDE}✔ Repositorio local inicializado.${NC}"
    fi
    git remote set-url origin "https://$W_USER:$W_TOKEN@github.com/$W_USER/$W_REPO.git" 2>/dev/null || \
    git remote add origin "https://$W_USER:$W_TOKEN@github.com/$W_USER/$W_REPO.git"

    if ! git config --get gitflow.branch.master > /dev/null; then git flow init -d > /dev/null; fi

    while true; true; do
        mostrar_header
        RAMA_ACTUAL=$(git rev-parse --abbrev-ref HEAD)
        echo -e "${CYAN}╭── [ 🛠️ ESPACIO DE TRABAJO ] ──────────────╮${NC}"
        echo -e "│ Repositorio: ${BLANCO}$W_REPO${NC}"
        echo -e "│ Rama actual: ${AMARILLO}$RAMA_ACTUAL${NC}"
        echo -e "│"
        echo -e "│ ${MAGENTA}1)${NC} Añadir archivos y hacer Commit Local"
        echo -e "│ ${MAGENTA}2)${NC} Iniciar NUEVA Función (Feature GitFlow)"
        echo -e "│ ${MAGENTA}3)${NC} TERMINAR Función (Une a Develop y sube)"
        echo -e "│ ${MAGENTA}4)${NC} LANZAR A PRODUCCIÓN (Push a MAIN)"
        echo -e "│ ${MAGENTA}5)${NC} Volver"
        echo -e "${CYAN}╰─────────────────────────────────────────╯${NC}"
        read -p "❯ Acción: " G_ACT

        case $G_ACT in
            1) 
               git add .
               read -p "📝 Mensaje del commit: " MSJ
               git commit -m "${MSJ:-"Actualizacion"}" -q
               git push -u origin "$RAMA_ACTUAL" 2>/dev/null
               echo -e "${VERDE}✔ Commit realizado en la rama $RAMA_ACTUAL.${NC}"; sleep 2 ;;
            2) 
               read -p "🚀 Nombre de la función (sin espacios): " FEAT
               git flow feature start "$FEAT"
               echo -e "${VERDE}✔ Estás en la rama feature/$FEAT. ¡A programar!${NC}"; sleep 2 ;;
            3) 
               if [[ "$RAMA_ACTUAL" == feature/* ]]; then
                   FEAT_NAME=${RAMA_ACTUAL#feature/}
                   git add .
                   git commit -m "Completado $FEAT_NAME" -q
                   git flow feature finish "$FEAT_NAME"
                   git push origin develop
                   echo -e "${VERDE}✔ Función fusionada a develop y subida a GitHub.${NC}"
               else
                   echo -e "${ROJO}⚠️ No estás en una rama Feature.${NC}"
               fi
               sleep 2 ;;
            4) 
               git checkout main
               git merge develop
               git push -u origin main
               git checkout develop
               echo -e "${VERDE}✔ ¡PRODUCCIÓN ACTUALIZADA! F-Droid puede leer la rama MAIN ahora.${NC}"
               sleep 3 ;;
            5) break ;;
        esac
    done
}

# ==========================================
# MENÚ PRINCIPAL
# ==========================================
mostrar_header
verificar_seguridad

while true; true; do
    mostrar_header
    echo -e "${CYAN}╭── [ 🖥️ PANEL MAESTRO ] ───────────────────╮${NC}"
    echo -e "│ ${AMARILLO}1)${NC} Trabajar en un Repositorio (Commits)"
    echo -e "│ ${AMARILLO}2)${NC} Administrar Usuarios"
    echo -e "│ ${AMARILLO}3)${NC} Administrar Repositorios"
    echo -e "│ ${AMARILLO}4)${NC} Hacer Copia de Seguridad Física"
    echo -e "│ ${AMARILLO}5)${NC} Salir del sistema"
    echo -e "${CYAN}╰─────────────────────────────────────────╯${NC}"
    read -p "❯ Ejecutar orden: " MAIN_OPT

    case $MAIN_OPT in
        1) trabajar_git ;;
        2) admin_usuarios ;;
        3) admin_repos ;;
        4) respaldo_local ;;
        5) clear; echo -e "${CYAN}¡Nos vemos, Developer! ( ˘ ³˘)♥${NC}\n"; exit 0 ;;
        *) echo "Opción inválida."; sleep 1 ;;
    esac
done
