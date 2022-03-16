helm install --set project_dir="$(pwd)" localstack localstack/helm/localstack
helm install postgresql postgresql/helm/postgresql
helm install dispatcher dispatcher/src/main/helm
helm install dns-crawler dns-crawler/src/main/helm
helm install content-crawler content-crawler/src/main/helm
helm install muppets muppets/src/main/helm/
helm install smtp-crawler smtp-crawler/src/main/helm
helm install vat-crawler vat-crawler/src/main/helm
