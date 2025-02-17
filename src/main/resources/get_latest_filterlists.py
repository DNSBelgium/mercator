import requests
import os

JSON_BASE_URL = "https://raw.githubusercontent.com/enthec/webappanalyzer/main/src/technologies/"

json_files = [
    "_.json", "a.json", "b.json", "c.json", "d.json", "e.json", "f.json", "g.json",
    "h.json", "i.json", "j.json", "k.json", "l.json", "m.json", "n.json", "o.json",
    "p.json", "q.json", "r.json", "s.json", "t.json", "u.json", "v.json", "w.json",
    "x.json", "y.json", "z.json"
]

output_dir_technologies = "technologies_backup/downloaded_jsons"
output_dir_normal = "technologies_backup"
os.makedirs(output_dir_technologies, exist_ok=True)
os.makedirs(output_dir_normal, exist_ok=True)

def download_file(url, output_path):
    response = requests.get(url)
    if response.status_code == 200:
        with open(output_path, "wb") as f:
            f.write(response.content)
        print(f"Downloaded {output_path}")
    else:
        print(f"Error downloading {output_path}: {response.status_code}")

def download_json_files():
    categories_url = "https://raw.githubusercontent.com/enthec/webappanalyzer/main/src/categories.json"
    groups_url = "https://raw.githubusercontent.com/enthec/webappanalyzer/main/src/groups.json"

    download_file(categories_url, f"{output_dir_normal}/categories.json")
    download_file(groups_url, f"{output_dir_normal}/groups.json")

    for i in json_files:
        url = JSON_BASE_URL + i
        output_path = f"{output_dir_technologies}/{i}"
        download_file(url, output_path)

download_json_files()