class EventHook:
    def __init__(self):
        self._handlers = []

    def add(self, handler):
        if handler not in self._handlers:
            self._handlers.append(handler)
        return self

    def remove(self, handler):
        if handler in self._handlers:
            self._handlers.remove(handler)
        return self

    def trigger(self, *args, **kwargs):
        for handler in self._handlers:
            try:
                handler(*args, **kwargs)
            except Exception as e:
                print(e)

    __iadd__ = add
    __isub__ = remove
    __call__ = trigger
