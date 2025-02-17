# -*- coding: utf-8 -*-
"""Use AWS4Auth and botocore to fetch credentials and sign requests."""
from botocore.credentials import get_credentials
from botocore.session import Session
from requests_aws4auth import AWS4Auth


def setup_aws_auth(region: str, profile: str = None) -> AWS4Auth:
    """For a given region sign a request to the execute-api with standard credentials."""
    credentials = get_credentials(Session(profile=profile))
    auth = AWS4Auth(credentials.access_key, credentials.secret_key, region, "execute-api", session_token=credentials.token)
    return auth
