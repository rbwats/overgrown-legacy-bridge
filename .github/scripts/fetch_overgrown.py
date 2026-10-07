"""Download the tested Overgrown Apoli and Origins jars from Modrinth into libs/ (CI only; never committed)."""
import io, json, os, sys, urllib.parse, urllib.request, zipfile

API = "https://api.modrinth.com/v2"
HEADERS = {"User-Agent": "rbwats/overgrown-legacy-bridge release workflow"}
GAME = "1.20.1"


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=60) as response:
        return response.read()


def versions(project):
    query = urllib.parse.urlencode({"game_versions": json.dumps([GAME]), "loaders": json.dumps(["fabric"])})
    return json.loads(get(f"{API}/project/{project}/version?{query}"))


def mod_id(jar):
    with zipfile.ZipFile(io.BytesIO(jar)) as archive:
        if "fabric.mod.json" not in archive.namelist():
            return None, False
        meta = json.loads(archive.read("fabric.mod.json"))
        return meta.get("id"), any(name.startswith("dev/overgrown/") for name in archive.namelist())


def fetch(label, projects, wanted, expected_id, target):
    seen = []
    for project in projects:
        try:
            candidates = versions(project)
        except Exception as error:  # an unknown slug or ID returns 404
            print(f"{label}: project {project!r} unavailable: {error}")
            continue
        for version in candidates:
            seen.append(f"{project}:{version['version_number']}")
            if not version["version_number"].startswith(wanted):
                continue
            primary = next((f for f in version["files"] if f.get("primary")), version["files"][0])
            jar = get(primary["url"])
            found_id, overgrown = mod_id(jar)
            if found_id != expected_id or not overgrown:
                print(f"{label}: skipping {primary['filename']} (id={found_id}, overgrown classes={overgrown})")
                continue
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, "wb") as out:
                out.write(jar)
            print(f"{label}: {project} {version['version_number']} -> {target} ({primary['filename']})")
            return
    print(f"{label}: no {wanted} release with mod id {expected_id!r}. Versions seen for {GAME}/fabric:")
    for entry in seen:
        print("  " + entry)
    sys.exit(1)


def search(query):
    facets = json.dumps([["project_type:mod"]])
    hits = json.loads(get(f"{API}/search?" + urllib.parse.urlencode({"query": query, "facets": facets, "limit": 20})))["hits"]
    for hit in hits:
        print(f"search {query!r}: {hit['slug']} ({hit['project_id']}) by {hit['author']}: {hit['title']}")
    return [hit["slug"] for hit in hits]


def sibling_projects(project):
    """Projects published by the same Modrinth team members as the given project."""
    slugs = []
    for member in json.loads(get(f"{API}/project/{project}/members")):
        user = member["user"]
        for other in json.loads(get(f"{API}/user/{user['id']}/projects")):
            print(f"{project} member {user['username']}: {other['slug']} ({other['id']}): {other['title']}")
            if other["slug"] not in slugs:
                slugs.append(other["slug"])
    return slugs


if __name__ == "__main__":
    apoli = os.environ.get("APOLI_VERSION", "1.90.0")
    origins = os.environ.get("ORIGINS_VERSION", "1.41.0")
    fetch("Apoli", ["opoli"] + search("overgrown apoli"), apoli, "apoli", "libs/apoli.jar")
    candidates = []
    for source in (lambda: sibling_projects("opoli"), lambda: search("overgrown origins"), lambda: search("origins overgrown")):
        try:
            candidates += [slug for slug in source() if slug not in candidates and slug != "opoli"]
        except Exception as error:
            print(f"Origins discovery step failed: {error}")
    # Search results sometimes put unrelated Origins forks first; the mod id and dev/overgrown classes decide.
    fetch("Origins", candidates, origins, "origins", "libs/origins.jar")
