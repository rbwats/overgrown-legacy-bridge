"""Starts a production-mapped Fabric client the way the vanilla launcher does, for the CI mixin audit.

Usage: launch_client.py <run dir> <minecraft version> <loader version> [extra JVM args...]
Mods must already be in <run dir>/mods. Only the asset index is fetched; sounds and other asset objects are
not needed to reach the title screen.
"""
import json
import os
import subprocess
import sys
import urllib.request

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"


def get(url):
    with urllib.request.urlopen(url, timeout=120) as response:
        return response.read()


def download(url, path):
    if os.path.exists(path):
        return path
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = get(url)
    with open(path + ".part", "wb") as out:
        out.write(data)
    os.replace(path + ".part", path)
    return path


def allowed(rules):
    if not rules:
        return True
    result = False
    for rule in rules:
        if "features" in rule:
            continue
        os_rule = rule.get("os")
        if os_rule is None or os_rule.get("name") == "linux":
            result = rule["action"] == "allow"
    return result


def maven_path(name):
    group, artifact, version, *classifier = name.split(":")
    suffix = f"-{classifier[0]}" if classifier else ""
    return f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}{suffix}.jar"


def main():
    run, mc, loader, *jvm_extra = sys.argv[1:]
    run = os.path.abspath(run)
    cache = os.path.join(run, ".launcher")
    versions = json.loads(get(MANIFEST))["versions"]
    version = json.loads(get(next(v["url"] for v in versions if v["id"] == mc)))
    profile = json.loads(get(f"https://meta.fabricmc.net/v2/versions/loader/{mc}/{loader}/profile/json"))

    # Fabric's libraries (ASM, Mixin, the loader) come first and win over same-named vanilla artifacts.
    classpath, seen = [], set()
    for library in profile["libraries"]:
        group, artifact = library["name"].split(":")[:2]
        seen.add((group, artifact))
        path = maven_path(library["name"])
        base = library.get("url", "https://maven.fabricmc.net/")
        classpath.append(download(base.rstrip("/") + "/" + path, os.path.join(cache, "libraries", path)))
    for library in version["libraries"]:
        if not allowed(library.get("rules")):
            continue
        parts = library["name"].split(":")
        if len(parts) == 3 and (parts[0], parts[1]) in seen:
            continue
        artifact = library.get("downloads", {}).get("artifact")
        if artifact:
            classpath.append(download(artifact["url"], os.path.join(cache, "libraries", artifact["path"])))
    client = download(version["downloads"]["client"]["url"], os.path.join(cache, "versions", mc, f"{mc}.jar"))
    classpath.append(client)

    assets = os.path.join(run, "assets")
    index = version["assetIndex"]
    download(index["url"], os.path.join(assets, "indexes", f"{index['id']}.json"))

    jvm = ["-Xmx2G", *[a for a in profile.get("arguments", {}).get("jvm", []) if isinstance(a, str)], *jvm_extra]
    game = ["--username", "BridgeAudit", "--version", mc, "--gameDir", run, "--assetsDir", assets,
            "--assetIndex", index["id"], "--uuid", "00000000000000000000000000000000", "--accessToken", "0",
            "--userType", "legacy", "--versionType", "release", "--width", "854", "--height", "480"]
    command = ["java", *jvm, "-cp", os.pathsep.join(classpath), profile["mainClass"], *game]
    print(f"Launching {profile['mainClass']} with {len(classpath)} classpath entries", flush=True)
    sys.exit(subprocess.call(command, cwd=run))


if __name__ == "__main__":
    main()
