import os
import uuid
import yaml
from locust import HttpUser, task, between, events
from locust.exception import LocustError


def load_config():
  config_path = os.getenv("GBIF_CONFIG_FILE", "config.yaml")

  if not os.path.exists(config_path):
    raise LocustError(f"Config file not found: {config_path}")

  with open(config_path, "r") as f:
    cfg = yaml.safe_load(f) or {}

  gbif = cfg.get("gbif", {})

  required = ["username", "password", "dataset_key"]
  missing = [k for k in required if not gbif.get(k)]

  if missing:
    raise LocustError(f"Missing GBIF config values: {', '.join(missing)}")

  return gbif


@events.init.add_listener
def validate_host(environment, **kwargs):
  """
  Fail fast if --host is not supplied.
  This runs before any users are spawned.
  """
  if not environment.host:
    raise LocustError(
        "Missing required --host parameter "
        "(e.g. --host=https://api.gbif.org)"
    )


class GBIFRegistryUser(HttpUser):
  wait_time = between(3, 7)

  def on_start(self):
    gbif = load_config()
    self.auth = (gbif["username"], gbif["password"])
    self.dataset_key = gbif["dataset_key"]

  @task
  def get_and_update_dataset(self):
    url = f"/v1/dataset/{self.dataset_key}"
    print(f"Requesting: {self.client.base_url}{url}")
    # 1. Retrieve dataset
    with self.client.get(
        f"/v1/dataset/{self.dataset_key}",
        auth=self.auth,
        name="getDataset",
        catch_response=True,
        timeout=120,  # 10 seconds timeout
    ) as get_resp:
      if get_resp.status_code == 0:
        get_resp.failure("Connection failed - server may be down")
        return
      if get_resp.status_code != 200:
        get_resp.failure(f"GET failed: {get_resp.status_code} {get_resp.text}")
        return

      dataset = get_resp.json()

    # 2. Modify title only
    dataset["title"] = f"Locust title update {uuid.uuid4()}"
    dataset["key"] = self.dataset_key

    # 3. Update dataset
    with self.client.put(
        f"/v1/dataset/{self.dataset_key}",
        json=dataset,
        auth=self.auth,
        name="updateDataset",
        catch_response=True,
    ) as put_resp:
      if put_resp.status_code not in (200, 201, 204):
        put_resp.failure(
            f"PUT failed: {put_resp.status_code} {put_resp.text}"
        )
      else:
        put_resp.success()
