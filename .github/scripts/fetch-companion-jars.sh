#!/usr/bin/env bash
# Скачивает из Modrinth игровые jar зависимостей NeoForge и Fabric.
# У Ribbits / GeckoLib один version_number бывает у обоих загрузчиков,
# поэтому выбор идёт и по loader, и по имени файла.
# Аргументы: <каталог> <KFF> <Ribbits> <GeckoLib> <YUNG NeoForge> <Patchouli NeoForge>
#            <Fabric API> <Fabric Language Kotlin> <Cloth Config> <YUNG Fabric> <Patchouli Fabric>
set -euo pipefail

OUT_DIR="${1:?destination directory}"
KFF_VERSION="${2:?kotlin-for-forge version_number}"
RIBBITS_VERSION="${3:?ribbits version_number}"
GECKOLIB_VERSION="${4:?geckolib version_number}"
YUNGSAPI_VERSION="${5:?yungs-api neoforge version_number}"
PATCHOULI_VERSION="${6:?patchouli neoforge version_number}"
FABRIC_API_VERSION="${7:?fabric-api version_number}"
FABRIC_KOTLIN_VERSION="${8:?fabric-language-kotlin version_number}"
CLOTH_VERSION="${9:?cloth-config version_number}"
YUNGSAPI_FABRIC_VERSION="${10:?yungs-api fabric version_number}"
PATCHOULI_FABRIC_VERSION="${11:?patchouli fabric version_number}"
USER_AGENT="${MODRINTH_USER_AGENT:-Nergan/really-useful-ribbits-mod (https://github.com/Nergan/really-useful-ribbits-mod)}"

mkdir -p "${OUT_DIR}"

fetch_modrinth() {
  local slug="$1"
  local version="$2"
  local loader="$3"
  local json url filename sha512

  json="$(curl -fsSL -A "${USER_AGENT}" "https://api.modrinth.com/v2/project/${slug}/version")"
  url="$(echo "${json}" | jq -r --arg v "${version}" --arg loader "${loader}" '
    first(
      .[]
      | select(.version_number == $v)
      | select(.loaders | index($loader))
      | .files[]
      | select(.primary)
      | .url
    ) // empty
  ')"
  filename="$(echo "${json}" | jq -r --arg v "${version}" --arg loader "${loader}" '
    first(
      .[]
      | select(.version_number == $v)
      | select(.loaders | index($loader))
      | .files[]
      | select(.primary)
      | .filename
    ) // empty
  ')"
  sha512="$(echo "${json}" | jq -r --arg v "${version}" --arg loader "${loader}" '
    first(
      .[]
      | select(.version_number == $v)
      | select(.loaders | index($loader))
      | .files[]
      | select(.primary)
      | .hashes.sha512
    ) // empty
  ')"

  if [[ -z "${url}" || -z "${filename}" || -z "${sha512}" ]]; then
    echo "No primary ${loader} Modrinth file for ${slug} version ${version}" >&2
    exit 1
  fi
  if [[ "${loader}" == "neoforge" && "${filename}" == *[Ff]abric* ]]; then
    echo "Refusing Fabric jar for NeoForge ${slug}: ${filename}" >&2
    exit 1
  fi
  if [[ "${loader}" == "fabric" && "${filename}" == *[Nn]eo[Ff]orge* ]]; then
    echo "Refusing NeoForge jar for Fabric ${slug}: ${filename}" >&2
    exit 1
  fi

  echo "Fetching ${slug} ${version} (${loader}) -> ${filename}"
  curl -fsSL -A "${USER_AGENT}" -o "${OUT_DIR}/${filename}" "${url}"
  echo "${sha512}  ${OUT_DIR}/${filename}" | sha512sum -c -
}

fetch_modrinth "kotlin-for-forge" "${KFF_VERSION}" "neoforge"
fetch_modrinth "ribbits" "${RIBBITS_VERSION}" "neoforge"
fetch_modrinth "geckolib" "${GECKOLIB_VERSION}" "neoforge"
fetch_modrinth "yungs-api" "${YUNGSAPI_VERSION}" "neoforge"
fetch_modrinth "patchouli" "${PATCHOULI_VERSION}" "neoforge"
fetch_modrinth "fabric-api" "${FABRIC_API_VERSION}" "fabric"
fetch_modrinth "fabric-language-kotlin" "${FABRIC_KOTLIN_VERSION}" "fabric"
fetch_modrinth "cloth-config" "${CLOTH_VERSION}" "fabric"
fetch_modrinth "ribbits" "${RIBBITS_VERSION}" "fabric"
fetch_modrinth "geckolib" "${GECKOLIB_VERSION}" "fabric"
fetch_modrinth "yungs-api" "${YUNGSAPI_FABRIC_VERSION}" "fabric"
fetch_modrinth "patchouli" "${PATCHOULI_FABRIC_VERSION}" "fabric"
